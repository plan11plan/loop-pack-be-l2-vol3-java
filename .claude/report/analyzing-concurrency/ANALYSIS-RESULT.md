# Concurrency Analysis Report

> 분석 일자: 2026-03-05
> 대상: commerce-api 전체 도메인 (Product, Coupon, Order, User, Brand, ProductLike)

---

## 목차

1. [전체 동시성 제어 구조 요약](#1-전체-동시성-제어-구조-요약)
2. [공유 가변 상태 식별](#2-공유-가변-상태-식별)
3. [락 전략 분석](#3-락-전략-분석)
4. [Race Condition 탐지](#4-race-condition-탐지)
5. [@Retryable + @Transactional 조합 분석](#5-retryable--transactional-조합-분석)
6. [트랜잭션 범위 vs 락 유지 시간](#6-트랜잭션-범위-vs-락-유지-시간)
7. [동시성 테스트 평가](#7-동시성-테스트-평가)
8. [식별된 문제점 및 위험도](#8-식별된-문제점-및-위험도)
9. [개선 제안](#9-개선-제안)

---

## 1. 전체 동시성 제어 구조 요약

### 아키텍처 개요

```
Controller (TX 없음, 동시성 제어 없음)
  └─ Facade (@Transactional 시작점, 일부 @Retryable)
       └─ Service (@Transactional — REQUIRED 기본값으로 Facade TX에 합류)
            └─ Repository (JPA, 벌크 UPDATE)
```

### 동시성 제어 기법 사용 현황

| 기법 | 사용 여부 | 적용 위치 |
|------|----------|----------|
| 낙관적 락 (@Version) | O | ProductModel, UserModel, CouponModel |
| 비관적 락 (@Lock) | **X** | 미사용 |
| @Retryable | O | OrderFacade.createOrder, CouponService.issue |
| 벌크 UPDATE (WHERE 조건) | O | OwnedCouponJpaRepository.useByIdWhenAvailable |
| DB Unique 제약조건 | O | ProductLikeModel (user_id, product_id), BrandModel (name) |
| 분산 락 (Redis) | **X** | 미사용 (Redis는 캐싱/Master-Replica 용도만) |
| synchronized / ReentrantLock | **X** | 미사용 |
| @Async | **X** | 미사용 |

### @Version(낙관적 락) 적용 현황

| Entity | @Version | @Retryable 연결 | 비고 |
|--------|----------|----------------|------|
| ProductModel | O | OrderFacade.createOrder (max=10) | 재고 동시성 제어 |
| CouponModel | O | CouponService.issue (max=50) | 쿠폰 발급 동시성 제어 |
| UserModel | O | 없음 (OrderFacade.createOrder가 간접 커버) | 포인트 동시성 — 직접 재시도 없음 |
| OrderModel | **X** | - | 주문 상태 변경 동시성 미보호 |
| OwnedCouponModel | **X** | - | 벌크 UPDATE WHERE 조건으로 대체 |
| BrandModel | **X** | - | 낮은 동시성, 불필요 |
| ProductLikeModel | **X** | - | Unique 제약조건으로 대체 |

---

## 2. 공유 가변 상태 식별

| Entity | 동시 쓰기 필드 | 동시 접근 시나리오 | 보호 기법 | 평가 |
|--------|--------------|------------------|----------|------|
| ProductModel | `stock` | 동시 주문으로 재고 차감 | @Version + @Retryable(max=10) | **적절** |
| CouponModel | `issuedQuantity` | 동시 쿠폰 발급 | @Version + @Retryable(max=50) | **조건부 적절** (아래 P1 참조) |
| UserModel | `point` | 동시 주문으로 포인트 차감 | @Version (재시도 없음) | **WARNING** |
| OwnedCouponModel | `orderId`, `usedAt` | 동시 주문으로 쿠폰 사용 | 벌크 UPDATE WHERE orderId IS NULL | **적절** |
| OrderModel | `status`, `totalPrice` | 동일 주문 아이템 동시 취소 | 보호 없음 (@Version 없음) | **WARNING** |
| ProductLikeModel | INSERT 자체 | 동일 상품 동시 좋아요 | Unique 제약 + 애플리케이션 중복 검사 | **적절** (DB 레벨 보호) |
| BrandModel | `name` | 동일 이름 브랜드 동시 등록 | Unique 제약 + 애플리케이션 중복 검사 | **적절** (저경합) |

---

## 3. 락 전략 분석

### 3.1 낙관적 락 (Optimistic Locking)

| Entity | @Version 타입 | @Retryable 위치 | maxAttempts | backoff | 평가 |
|--------|-------------|----------------|-------------|---------|------|
| ProductModel | Long | OrderFacade.createOrder | 10 | 50ms random | 적절 |
| CouponModel | Long | CouponService.issue | 50 | 50ms random | **위험** — Facade TX 합류 문제 |
| UserModel | Long | 없음 | - | - | **미비** — 직접 재시도 경로 없음 |

**CouponModel 고경합 분석:**
- 50개 수량 제한 쿠폰에 100명 동시 요청 시나리오
- maxAttempts=50 + random backoff로 대부분 성공 기대
- 단, @Retryable이 CouponService에 있고 Facade TX에 합류하는 구조적 문제 존재 (섹션 5 참조)

### 3.2 비관적 락 (Pessimistic Locking)

**미사용.** 프로젝트 전체에서 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 등의 선언 없음.

### 3.3 DB 수준 제어

#### 벌크 UPDATE (원자적 상태 전이)

**OwnedCouponJpaRepository.useByIdWhenAvailable:**
```sql
UPDATE OwnedCouponModel o
SET o.orderId = :orderId, o.usedAt = :usedAt
WHERE o.id = :id AND o.orderId IS NULL
```
- `WHERE orderId IS NULL` 조건으로 원자적 "사용 가능 → 사용됨" 전이
- 반환값 0이면 이미 사용된 쿠폰 → `ALREADY_USED` 예외
- `@Modifying(flushAutomatically = true, clearAutomatically = true)` 적용
- **평가:** 동시 쿠폰 사용 방지에 효과적. @Version 없이도 안전.

#### Unique 제약조건

| Entity | 제약 이름 | 컬럼 | 용도 |
|--------|---------|------|------|
| ProductLikeModel | `uk_likes_user_product` | (user_id, product_id) | 중복 좋아요 방지 |
| BrandModel | (컬럼 단위) | name | 중복 브랜드명 방지 |

### 3.4 애플리케이션 수준 제어

**미사용.** `synchronized`, `ReentrantLock`, `Atomic*` 등은 프로덕션 코드에 없음. (테스트 코드의 `AtomicInteger`/`AtomicLong`은 테스트 카운터/ID 생성 용도)

### 3.5 분산 락

**미사용.** Redis는 Master-Replica 캐싱 구성만 존재.
- `RedisConfig`에 `defaultRedisTemplate` (Replica 선호)과 `masterRedisTemplate` (Master 강제) 정의
- Redisson, SET NX 등의 분산 락 패턴 없음

---

## 4. Race Condition 탐지

### Race Condition 후보

| 위치 | 패턴 | 경합 시나리오 | 보호 기법 | 위험도 |
|------|------|-------------|----------|--------|
| CouponService.issue | Read-then-Write | 동시 발급 → issuedQuantity 경합 | @Version | **LOW** (보호됨) |
| CouponService.issue | Check-then-Act | 중복 발급 검사 → save 사이 갭 | @Version 재시도로 재검사 | **LOW** (보호됨) |
| ProductLikeService.like | Check-then-Act | findBy... → save 사이 동시 INSERT | DB Unique 제약 | **LOW** (DB 보호) |
| UserService.deductPoint | Read-then-Write | 동시 주문 → 포인트 경합 | @Version (재시도 없음) | **MEDIUM** |
| OrderFacade.cancelMyOrderItem | Read-then-Write | 동일 주문 아이템 동시 취소 | 보호 없음 | **MEDIUM** |
| BrandService.register | Check-then-Act | 동일 이름 동시 등록 | DB Unique 제약 | **LOW** (DB 보호) |

### 상세 분석

#### 4.1 UserModel.deductPoint — Lost Update 위험

```
Thread A (주문 50,000원): SELECT user → point=100,000, version=1
Thread B (주문 60,000원): SELECT user → point=100,000, version=1
Thread A: UPDATE user SET point=50,000, version=2 WHERE version=1 → 성공
Thread B: UPDATE user SET point=40,000, version=2 WHERE version=1 → 실패 (OptimisticLockingFailureException)
```

- @Version이 있으므로 Lost Update는 방지됨
- 그러나 Thread B의 `ObjectOptimisticLockingFailureException`을 잡아 재시도하는 메커니즘이 `deductPoint` 직접 경로에는 없음
- **OrderFacade.createOrder의 @Retryable이 간접 커버**: 주문 생성 전체를 재시도하므로 포인트 차감 실패도 재시도됨
- **직접 포인트 차감 API가 생기면 문제**: 향후 포인트 충전/사용 API가 별도로 추가되면 @Retryable 없이 실패

#### 4.2 OrderModel — 동시 취소 시 상태 불일치

```
Thread A: cancelMyOrderItem(order, item1) → status 변경, totalPrice 재계산
Thread B: cancelMyOrderItem(order, item2) → 동일 order 조회 (Thread A의 변경 전 상태)
```

- OrderModel에 @Version이 없으므로 Dirty Checking 기반 UPDATE가 충돌 없이 실행됨
- Thread A와 B가 동시에 `recalculateTotalPrice()`를 수행하면 한쪽의 계산 결과가 덮어씌워질 수 있음
- cancelMyOrderItem에 @Retryable이 없으므로 재시도도 불가

#### 4.3 ProductLikeService.like — 비즈니스 예외 vs DB 예외

```
Thread A: findByUserIdAndProductId → 없음
Thread B: findByUserIdAndProductId → 없음
Thread A: save() → 성공
Thread B: save() → DataIntegrityViolationException (Unique 위반)
```

- DB Unique 제약으로 데이터 정합성은 보장됨
- 단, 사용자에게 `CONFLICT` 비즈니스 예외가 아닌 `DataIntegrityViolationException`이 전달됨
- 실질적 위험은 낮으나, 예외 타입 불일치가 클라이언트 에러 핸들링에 영향

---

## 5. @Retryable + @Transactional 조합 분석

### 5.1 OrderFacade.createOrder — 정상 동작

```
호출 체인:
OrderV1Controller.create() [TX 없음]
  └─ OrderFacade.createOrder() [@Retryable(max=10) + @Transactional]
       ├─ productService.validateAndDeductStock() [@Transactional → REQUIRED, 합류]
       ├─ brandService.getNameMapByIds()
       ├─ orderService.createOrder() [@Transactional → REQUIRED, 합류]
       ├─ couponService.useAndCalculateDiscount() [@Transactional → REQUIRED, 합류]
       ├─ orderService.applyDiscount() [@Transactional → REQUIRED, 합류]
       └─ userService.deductPoint() [@Transactional → REQUIRED, 합류]
```

- Controller에 @Transactional 없음 → **OrderFacade가 TX 시작점**
- @Retryable이 TX 시작점과 같은 메서드에 있으므로, 재시도 시 새 TX가 생성됨
- `ObjectOptimisticLockingFailureException`은 TX 커밋 시점에 발생하고, @Retryable이 이를 포착
- **평가: OK** — 프록시 순서가 올바르게 동작하는 구조

### 5.2 CouponService.issue — 구조적 문제

```
호출 체인:
CouponV1Controller.issue() [TX 없음]
  └─ CouponFacade.issueCoupon() [@Transactional]        ← TX 시작점
       └─ CouponService.issue() [@Retryable(max=50) + @Transactional(REQUIRED)]
                                                         ← Facade TX에 합류
```

**문제 분석:**

1. `CouponFacade.issueCoupon()`의 `@Transactional`이 TX를 시작
2. `CouponService.issue()`의 `@Transactional(REQUIRED)`는 기본값 → **Facade TX에 합류**
3. `ObjectOptimisticLockingFailureException`은 **Facade TX 커밋 시점**에 발생
4. Service의 `@Retryable`은 **Service 메서드 반환 시점**에서만 예외를 감지
5. Service 메서드 내에서는 예외가 발생하지 않으므로, **@Retryable이 트리거되지 않을 가능성**

**그런데 왜 동시성 테스트가 통과하는가?**

Spring AOP 프록시 동작 방식을 면밀히 분석하면:
- `@Transactional` 프록시는 메서드 진입 시 기존 TX가 있으면 합류, 없으면 새 TX 시작
- `@Retryable` 프록시는 `@Transactional` 프록시를 감싸고 있음
- Facade TX에 합류한 상태에서 `coupon.issue()` → `issuedQuantity++` 변경
- Service 메서드 반환 시, **합류한 TX에서 flush가 발생하지 않음** (커밋은 Facade에서)
- 따라서 Service 반환 시점에는 예외가 없고, Facade 커밋 시점에 예외 발생
- Facade에는 @Retryable이 없으므로, 예외는 Controller까지 전파됨

**실제 동작 확인 필요:**
- 동시성 테스트(`CouponIssueConcurrencyTest`)가 통과한다면, flush 타이밍이나 프록시 순서가 예상과 다를 수 있음
- 또는 `@Version` 충돌이 `findById` 시점의 SELECT 자체에서 감지되는 경우 (이전 TX에서 변경된 version을 읽는 경우)

**위험 시나리오:**
- 현재 테스트가 통과하더라도, Spring Boot 버전 업그레이드 시 프록시 순서 변경 가능
- 프록시 Order가 명시적으로 설정되지 않아 동작이 보장되지 않음

### 조합 요약

| 위치 | @Retryable | @Transactional | 전파 속성 | 상위 TX | 재시도 동작 | 평가 |
|------|-----------|---------------|----------|--------|-----------|------|
| OrderFacade.createOrder | max=10 | REQUIRED | TX 시작점 | 없음 | **정상** | OK |
| CouponService.issue | max=50 | REQUIRED | Facade TX 합류 | CouponFacade | **위험** | WARNING |

---

## 6. 트랜잭션 범위 vs 락 유지 시간

### 낙관적 락에서 TX 길이의 의미

낙관적 락(@Version)은 실제 락을 잡지 않고 커밋 시 충돌을 검증한다. 따라서:
- TX가 길수록 **다른 TX와 충돌할 확률이 높아짐** (같은 row를 동시에 읽고 쓸 시간 창이 넓어짐)
- 충돌 시 전체 TX가 롤백되므로, TX 내 작업이 많을수록 **롤백 비용이 커짐**

### 유즈케이스별 분석

| 유즈케이스 | TX 범위 | 락 대상 | 락 유형 | TX 내 작업 수 | 불필요 포함 작업 | 평가 |
|-----------|--------|--------|--------|-------------|---------------|------|
| 주문 생성 | OrderFacade | ProductModel, UserModel | @Version | 6 | 브랜드명 조회 | **WARNING** |
| 쿠폰 발급 | CouponFacade→Service | CouponModel | @Version | 3 | 없음 | OK |
| 쿠폰 사용 | OrderFacade (주문 내) | OwnedCouponModel | 벌크 UPDATE | 1 | 없음 | OK |
| 주문 취소 | OrderFacade | ProductModel | @Version | 3~4 | 없음 | **WARNING** (재시도 없음) |

### 주문 생성 TX 상세

```
OrderFacade.createOrder() [단일 TX]
  ├─ [1] productService.validateAndDeductStock()   [쓰기] ProductModel @Version 충돌 가능
  ├─ [2] brandService.getNameMapByIds()             [읽기] 스냅샷 용도 — TX 불필요
  ├─ [3] orderService.createOrder()                 [쓰기] INSERT
  ├─ [4] couponService.useAndCalculateDiscount()    [쓰기] 벌크 UPDATE
  ├─ [5] orderService.applyDiscount()               [쓰기] Dirty Checking
  └─ [6] userService.deductPoint()                  [쓰기] UserModel @Version 충돌 가능
```

- [1]에서 ProductModel에 대한 @Version 충돌 시, [2]~[6]의 작업이 모두 롤백
- [6]에서 UserModel에 대한 @Version 충돌 시, [1]~[5]의 작업이 모두 롤백
- 최대 @Retryable(max=10)으로 재시도하지만, 재시도마다 [1]~[6] 전체를 다시 실행

---

## 7. 동시성 테스트 평가

### 테스트 커버리지

| 시나리오 | 테스트 존재 | 동시 출발 | 스레드 수 | 반복 | 검증 항목 | 평가 |
|---------|-----------|----------|----------|------|---------|------|
| 쿠폰 발급 (수량 제한) | O | O (4가지 방식 비교) | 50 | 3회 | 초과 발급, issuedQuantity-OwnedCoupon 정합성 | **우수** |
| 동일 쿠폰 동시 주문 | O | O (강화 패턴) | 30 | 3회 | 1건만 성공, 쿠폰 사용 상태 | **우수** |
| 동일 상품 동시 주문 (재고) | O | O (강화 패턴) | 30 | 3회 | 재고 정합성, 초과 차감 방지, 재고 >= 0 | **우수** |
| 포인트 동시 차감 | **X** | - | - | - | - | **미비** |
| 동시 좋아요 중복 방지 | **X** | - | - | - | - | **미비** |
| 동일 주문 동시 취소 | **X** | - | - | - | - | **미비** |

### 기존 테스트의 강점

1. **CouponIssueConcurrencyTest**: 4가지 동시성 테스트 패턴(Thread, 기본 Latch, 강화 Latch, CompletableFuture)을 비교하는 교육적 구조
2. **강화 CountDownLatch 패턴**: `readyLatch` + `startLatch` + `doneLatch` 3단계로 최대 경합 재현
3. **@RepeatedTest(3)**: 비결정적 테스트의 신뢰도 향상
4. **ConcurrencyResult record**: 검증 로직 재사용 + 콘솔 리포트 출력

### 누락된 테스트 시나리오

#### A. 포인트 동시 차감
- **시나리오:** 한 유저(point=100,000)가 동시에 여러 주문 (각 40,000원) 시도
- **기대:** 최대 2건 성공 (80,000원), 나머지 실패 (포인트 부족)
- **현재 위험:** @Version이 있어 Lost Update는 방지되나, 직접 재시도 없이 OrderFacade.createOrder의 @Retryable에 의존

#### B. 동시 좋아요
- **시나리오:** 같은 유저가 같은 상품에 동시 좋아요 요청
- **기대:** 1건만 성공
- **현재 위험:** DataIntegrityViolationException이 비즈니스 예외 대신 발생

#### C. 동일 주문 동시 아이템 취소
- **시나리오:** 같은 주문의 item1과 item2를 동시 취소
- **기대:** 둘 다 성공하되, totalPrice와 status가 정확히 반영
- **현재 위험:** OrderModel에 @Version 없음, 재시도 없음 → totalPrice 오계산 가능

---

## 8. 식별된 문제점 및 위험도

### [WARNING] P1: CouponService.issue — @Retryable + @Transactional 계층 배치 문제

**현상:**
```java
// CouponFacade — TX 시작점
@Transactional
public CouponResult.IssuedDetail issueCoupon(Long couponId, Long userId) {
    return CouponResult.IssuedDetail.from(couponService.issue(couponId, userId));
}

// CouponService — Facade TX에 합류 (REQUIRED)
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 50)
@Transactional
public OwnedCouponModel issue(Long couponId, Long userId) { ... }
```

**문제:**
- @Retryable이 Service에 있으나, TX 커밋은 Facade에서 발생
- Service 메서드 반환 시점에는 @Version 충돌 예외가 아직 발생하지 않음
- @Retryable이 예외를 포착하지 못할 가능성

**실제 동작:**
- 현재 동시성 테스트가 통과하므로, 프록시 순서나 flush 타이밍에 의해 동작 중일 수 있음
- 그러나 이 동작은 Spring 내부 구현에 의존하며, 버전 업그레이드 시 변경 가능

---

### [WARNING] P2: UserModel.deductPoint — 독립 재시도 경로 부재

**현상:**
- UserModel에 `@Version`이 있어 동시 수정 시 `ObjectOptimisticLockingFailureException` 발생
- `deductPoint`를 직접 호출하는 경로에 @Retryable 없음
- 현재는 `OrderFacade.createOrder`의 @Retryable(max=10)이 간접 커버

**위험:**
- 향후 포인트 충전/사용 API가 별도로 추가되면, @Retryable 없이 실패
- 현재도 `OrderFacade.createOrder`에서 재시도 시 재고 차감 + 쿠폰 사용이 모두 재실행되는 비용

---

### [WARNING] P3: OrderModel — @Version 미적용 + 동시 취소 위험

**현상:**
- `OrderModel`에 `@Version` 필드 없음
- `cancelMyOrderItem()`에 `@Retryable` 없음
- 같은 주문의 여러 아이템을 동시 취소 시 `recalculateTotalPrice()` 결과가 덮어씌워질 수 있음

**시나리오:**
```
Order (item1=10000, item2=20000, totalPrice=30000)

Thread A: cancel item1 → recalculate → totalPrice = 20000
Thread B: cancel item2 → recalculate → totalPrice = 10000
                                        (Thread A의 item1 취소가 반영 안 됨)

DB 결과: item1=CANCELLED, item2=CANCELLED, totalPrice=10000 (정답: 0)
```

**현실적 위험도:**
- 동일 주문의 여러 아이템을 정확히 같은 시점에 취소하는 시나리오는 드묾
- 관리자 일괄 취소나 API 중복 호출 시 발생 가능

---

### [INFO] P4: ProductLikeService.like — 예외 타입 불일치

**현상:**
```java
// 애플리케이션 레벨 검사 → CONFLICT 비즈니스 예외
if (productLikeRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
    throw new CoreException(ErrorType.CONFLICT, "이미 좋아요한 상품입니다");
}
// 동시 INSERT 시 → DataIntegrityViolationException (DB Unique 위반)
productLikeRepository.save(ProductLikeModel.create(userId, productId));
```

**영향:**
- 정상적인 단일 요청: `CONFLICT` 비즈니스 예외 반환
- 동시 요청 경합: `DataIntegrityViolationException` → 500 Internal Server Error
- 데이터 정합성 자체는 DB Unique 제약으로 보장됨

---

### [INFO] P5: OrderFacade.createOrder — 과도한 TX 범위

**현상:**
- 6개 서비스 호출이 단일 TX에 포함
- 브랜드명 조회(`brandService.getNameMapByIds`)는 스냅샷 용도로 TX 외부 가능
- TX 길이가 길수록 @Version 충돌 확률 증가, 롤백 비용 증가

---

## 9. 개선 제안

### [제안 1] CouponService.issue — @Retryable 위치 조정

**현재 구조:**
```
CouponFacade [@Transactional] → CouponService [@Retryable + @Transactional(REQUIRED)]
```

**개선안 A — Facade에 @Retryable 이동:**
```java
// CouponFacade
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 50,
           backoff = @Backoff(delay = 50, random = true))
@Transactional
public CouponResult.IssuedDetail issueCoupon(Long couponId, Long userId) {
    return CouponResult.IssuedDetail.from(couponService.issue(couponId, userId));
}

// CouponService — @Retryable 제거
@Transactional
public OwnedCouponModel issue(Long couponId, Long userId) { ... }
```

**Trade-off:**
- 장점: @Retryable이 TX 시작점에 위치하여 프록시 순서 보장
- 장점: OrderFacade.createOrder과 동일한 패턴으로 일관성 확보
- 단점: Facade에 인프라 관심사(@Retryable) 추가

**개선안 B — Service에 REQUIRES_NEW 적용:**
```java
// CouponService
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 50)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public OwnedCouponModel issue(Long couponId, Long userId) { ... }
```

**Trade-off:**
- 장점: Service 내에서 독립 TX → @Retryable이 충돌 포착 가능
- 단점: Facade TX와 독립이므로, 쿠폰 발급 후 Facade 롤백 시 발급만 남는 부분 커밋 위험
- 현재 Facade가 단일 서비스만 호출하므로 위험은 제한적

**권장: 개선안 A** — OrderFacade.createOrder과 동일한 패턴으로 통일

---

### [제안 2] OrderModel — @Version 추가 또는 동시 취소 방어

**현재 구조:**
```java
// OrderModel — @Version 없음
public class OrderModel extends BaseEntity {
    // ...
}

// OrderFacade — @Retryable 없음
@Transactional
public void cancelMyOrderItem(Long userId, Long orderId, Long orderItemId) { ... }
```

**개선안 A — @Version 추가 + @Retryable:**
```java
// OrderModel
@Version
private Long version;

// OrderFacade
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
           maxAttempts = 5, backoff = @Backoff(delay = 50, random = true))
@Transactional
public void cancelMyOrderItem(Long userId, Long orderId, Long orderItemId) { ... }
```

**Trade-off:**
- 장점: 동시 취소 시 정합성 보장
- 단점: 정상적인 단일 취소 요청에도 version 검증 오버헤드 (미미)
- 고려: 재시도 시 재고 복구(increaseStock)도 재실행 — ProductModel @Version과 이중 경합 가능

**개선안 B — 비관적 락으로 주문 조회:**
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT o FROM OrderModel o WHERE o.id = :id")
Optional<OrderModel> findByIdForUpdate(@Param("id") Long id);
```

**Trade-off:**
- 장점: 동시 접근 자체를 차단하여 정합성 확실 보장
- 단점: 대기 시간 발생, 데드락 위험 (ProductModel과 순서 주의)

---

### [제안 3] ProductLikeService.like — DataIntegrityViolationException 처리

**현재:** 동시 요청 시 500 에러 반환

**개선안:**
```java
@Transactional
public void like(Long userId, Long productId) {
    if (productLikeRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
        throw new CoreException(ErrorType.CONFLICT, "이미 좋아요한 상품입니다");
    }
    try {
        productLikeRepository.save(ProductLikeModel.create(userId, productId));
    } catch (DataIntegrityViolationException e) {
        throw new CoreException(ErrorType.CONFLICT, "이미 좋아요한 상품입니다");
    }
}
```

**Trade-off:**
- 장점: 동시 요청에서도 일관된 비즈니스 예외 반환
- 단점: `DataIntegrityViolationException`은 TX 롤백 마킹 가능 → `@Transactional` 내에서 catch 후 다른 예외를 던지면 TX 상태에 주의 필요
- 대안: `saveAndFlush()` + try-catch, 또는 Facade 레벨에서 예외 변환

---

### [제안 4] 누락된 동시성 테스트 추가

**우선순위 순:**

1. **포인트 동시 차감 테스트** (P2 검증)
   - 한 유저 point=100,000, 30명 동시 40,000원 주문
   - 기대: 최대 2건 성공, 포인트 >= 0, 음수 방지

2. **동일 주문 동시 아이템 취소 테스트** (P3 검증)
   - 3개 아이템 주문, 동시에 3개 모두 취소
   - 기대: totalPrice 정합성, status = CANCELLED

3. **동시 좋아요 테스트** (P4 검증)
   - 같은 유저가 같은 상품에 30회 동시 좋아요
   - 기대: 정확히 1건만 성공

---

## 부록: Kafka / Spring Batch 동시성

### Kafka Consumer

| 설정 | 값 | 비고 |
|------|-----|------|
| concurrency | 3 | 3개 스레드 동시 소비 |
| ackMode | MANUAL | 수동 커밋 |
| batchListener | true | 배치 처리 |
| maxPollRecords | 3000 | 한 번에 최대 3000 메시지 |
| maxPollIntervalMs | 120,000ms | 2분 |
| sessionTimeoutMs | 60,000ms | 1분 |
| heartbeatIntervalMs | 20,000ms | session_timeout의 1/3 |

- 현재 DemoKafkaConsumer만 존재하며, 프로덕션 비즈니스 로직 미구현
- 동시성 이슈는 Consumer 비즈니스 로직 구현 시 점검 필요

### Spring Batch

- TaskExecutor 미설정 → 단일 스레드 실행
- DemoJob만 존재 (Tasklet 기반, ResourcelessTransactionManager)
- 동시성 이슈 없음