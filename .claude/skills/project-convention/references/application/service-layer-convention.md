# 서비스 계층 책임 분리 컨벤션

## 목차

1. [계층 구조 개요](#1-계층-구조-개요)
2. [Facade — Application 계층](#2-facade--application-계층)
3. [Domain Service — Domain 계층](#3-domain-service--domain-계층)
4. [트랜잭션 규칙](#4-트랜잭션-규칙)
5. [계층 간 호출 규칙](#5-계층-간-호출-규칙)
6. [Facade가 커질 때](#6-facade가-커질-때)
7. [체크리스트](#체크리스트)

---

## 1. 계층 구조 개요

```
Controller
    ↓
Facade (@Transactional)              ← Application 계층: 유스케이스 조율
    ├── OrderService (@Transactional)    ← Domain 계층: 자기 도메인 비즈니스 로직
    ├── ProductService (@Transactional)  ← Domain 계층: 타 도메인 Service 호출 가능
    └── MemberService (@Transactional)
```

| 계층 | 클래스 | 역할 |
|------|--------|------|
| **Application** | `{Domain}Facade` | 유스케이스 조율, 도메인 간 흐름 조합, DTO 변환 |
| **Domain** | `{Domain}Service` | 자기 도메인 비즈니스 로직, Repository 접근, Entity 조작 |

Application 계층에는 **Facade만 둔다**. 별도 ApplicationService 개념을 두지 않는다.

---

## 2. Facade — Application 계층

### 역할

- **유스케이스 조율**: 여러 Domain Service를 호출하여 하나의 비즈니스 흐름을 완성한다
- **DTO 변환**: Interface ↔ Application DTO 변환의 중간 지점
- **도메인 간 데이터 조합**: 여러 도메인의 Info를 Result로 묶어 반환한다
- **트랜잭션 경계**: 여러 Domain Service 호출의 원자성을 보장한다

### Facade에 넣는 것

- 여러 Domain Service 조합 흐름
- Entity → Info 변환, 여러 Info → Result 조합
- 타 도메인 Entity → Data DTO 변환 후 자기 도메인 Service에 전달

### Facade에 넣지 않는 것

- 비즈니스 규칙, 검증 로직 → Domain Service 또는 Entity
- Repository 직접 호출 → Domain Service
- Entity 상태 변경 로직 → Entity 메서드

### 예시

```java
@Service
public class OrderFacade {

    private final OrderService orderService;
    private final ProductService productService;
    private final MemberService memberService;

    @Transactional
    public OrderInfo createOrder(OrderCommand.Create command) {
        // 1. 타 도메인에서 필요한 데이터 조회
        MemberInfo member = memberService.getMember(command.memberId());
        List<ProductInfo> products = productService.getProducts(command.productIds());

        // 2. 타 도메인 Info → 자기 도메인 Data 변환
        OrderMemberData memberData = OrderMemberData.from(member);
        List<OrderProductData> productData = products.stream()
            .map(OrderProductData::from)
            .toList();

        // 3. 자기 도메인 Service에 위임
        Order order = orderService.create(memberData, productData);

        // 4. Entity → Info 변환 후 반환
        return OrderInfo.from(order);
    }

    @Transactional(readOnly = true)
    public OrderDetailResult getOrderDetail(OrderQuery.Detail query) {
        OrderInfo order = orderService.getOrderInfo(query.orderId());
        ProductInfo product = productService.getProduct(order.productId());
        MemberInfo member = memberService.getMember(order.memberId());

        return new OrderDetailResult(order, product, member);
    }
}
```

---

## 3. Domain Service — Domain 계층

### 역할

- **자기 도메인 비즈니스 로직**: Entity 생성, 상태 변경, 검증
- **Repository 접근**: 조회, 저장, 삭제
- **도메인 규칙 실행**: Entity만으로 완결되지 않는 로직 (Repository 조회 필요, 여러 Entity 조율)

### Domain Service에 넣는 것

- Repository를 통한 Entity 조회/저장
- Entity 생성 → 저장 흐름
- Entity 자기 필드만으로 완결되지 않는 비즈니스 규칙
- 같은 도메인 내 여러 Entity 조율

### Domain Service에 넣지 않는 것

- 타 도메인 Facade/Service 호출 → Facade에서 조율
- DTO 변환 (Info → Response 등) → Facade 또는 Interface 계층
- Entity 자기 필드만으로 완결되는 로직 → Entity 메서드

### CUD 메서드는 void를 반환한다

Domain Service의 생성/수정/삭제(CUD) 메서드는 **void**를 반환한다. 상위 계층(Facade)에서 필요하면 별도 조회 메서드를 호출한다.

- YAGNI: 반환값이 필요할 때 추가하면 된다
- 명령과 조회를 분리하여 메서드의 의도가 명확해진다

### 예시

```java
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public void create(OrderMemberData member, List<OrderProductData> products) {
        List<OrderLine> lines = products.stream()
            .map(p -> OrderLine.create(p.productId(), p.name(), p.price()))
            .toList();
        Order order = Order.create(member.memberId(), lines);
        orderRepository.save(order);
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = getOrder(orderId);
        order.cancel();  // Entity에 위임
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public OrderInfo getOrderInfo(Long orderId) {
        return OrderInfo.from(getOrder(orderId));
    }
}
```

---

## 4. 트랜잭션 규칙

### 메서드 레벨에 @Transactional을 붙인다

클래스 레벨이 아닌 **메서드 레벨**에 붙인다. 각 메서드의 트랜잭션 성격을 명시적으로 표현하기 위함이다.

```java
@Service
public class OrderService {

    @Transactional                        // 변경
    public Order create(...) { ... }

    @Transactional                        // 변경
    public void cancel(Long orderId) { ... }

    @Transactional(readOnly = true)       // 조회
    public Order getOrder(Long id) { ... }
}
```

클래스 레벨에 붙이지 않는 이유:
- 조회 메서드에도 불필요한 flush가 발생한다
- 메서드별 트랜잭션 성격이 코드에서 보이지 않는다
- 트랜잭션이 불필요한 메서드까지 포함될 수 있다

### 조회는 readOnly = true

조회 전용 메서드에는 반드시 `@Transactional(readOnly = true)`를 붙인다.

이점:
- JPA dirty checking flush 생략 → 성능 향상
- DB 읽기 전용 힌트 → DB 최적화 가능
- 실수로 변경 로직이 포함되면 예외 발생 → 안전장치

### Facade + Domain Service 양쪽 모두 @Transactional

```
Facade (@Transactional)                  ← 트랜잭션 시작
  └── OrderService (@Transactional)      ← 기존 트랜잭션에 참여 (REQUIRED 전파)
  └── ProductService (@Transactional)    ← 기존 트랜잭션에 참여
```

- Spring 기본 전파 옵션은 `REQUIRED` — 기존 트랜잭션이 있으면 참여, 없으면 새로 시작
- Facade에서 시작한 트랜잭션에 Domain Service들이 참여한다
- Domain Service를 단독 호출하면 자기가 트랜잭션을 시작한다
- **어디서 호출하든 트랜잭션이 보장된다**

### readOnly 전파 주의

```java
// ⚠️ 주의: Facade가 readOnly인데 하위가 변경하는 경우
@Transactional(readOnly = true)  // Facade
public OrderDetailResult getOrderDetail(...) {
    orderService.updateViewCount(...);  // ❌ readOnly 트랜잭션 안에서 변경 시도 → 문제
}
```

Facade의 readOnly가 하위로 전파되므로, 조회 Facade에서 변경 Service를 호출하면 안 된다.

---

## 5. 계층 간 호출 규칙

### 허용되는 호출

```
Controller → Facade                       ✅
Facade → 자기 도메인 Service               ✅
Facade → 타 도메인 Service                 ✅
Domain Service → 자기 도메인 Repository     ✅
Entity → Entity (같은 Aggregate 내부)      ✅
```

### 금지되는 호출

```
Facade → 타 Facade                        ❌  순환 의존, 트랜잭션 경계 혼란
Domain Service → 타 도메인 Service          ❌  도메인 간 결합
Domain Service → Facade                    ❌  하위 → 상위 역방향
Domain Service → Repository (타 도메인)     ❌  도메인 간 결합
Controller → Domain Service 직접            ❌  Facade 우회
```

### Facade가 타 도메인에 접근하는 방법

Facade는 타 도메인의 **Domain Service**를 직접 주입받아 호출한다.

```java
// ✅ 타 도메인 Service 직접 호출
@Service
public class OrderFacade {
    private final OrderService orderService;      // 자기 도메인
    private final ProductService productService;  // 타 도메인
    private final MemberService memberService;    // 타 도메인
}

// ❌ 타 Facade 호출 금지
@Service
public class OrderFacade {
    private final ProductFacade productFacade;    // 금지
}
```

---

## 6. Facade가 커질 때

Facade가 비대해지면 **유스케이스 단위로 분리**한다. ApplicationService 개념을 도입하지 않는다.

```java
// 처음: 하나의 Facade
OrderFacade

// 커지면: 유스케이스별 분리
OrderCommandFacade    // 생성, 취소, 변경
OrderQueryFacade      // 조회, 검색, 목록
```

분리 기준:
- Command(변경)와 Query(조회)가 자연스러운 첫 번째 분리 지점
- 그래도 크면 유스케이스 단위로 더 분리 (OrderCheckoutFacade, OrderReturnFacade 등)

---

## 체크리스트

**Facade**
- [ ] Facade에 비즈니스 규칙/검증 로직이 없는가? (Domain Service나 Entity에 있어야 함)
- [ ] Facade에서 Repository를 직접 호출하지 않는가?
- [ ] Facade가 타 Facade를 호출하지 않는가?
- [ ] 타 도메인 접근 시 타 도메인의 Domain Service를 직접 호출하는가?

**Domain Service**
- [ ] 자기 도메인의 Repository만 접근하는가?
- [ ] 타 도메인 Service나 Repository를 직접 호출하지 않는가?
- [ ] Entity만으로 완결되는 로직이 Entity에 있는가? (Service에 빼앗지 않았는가)

**트랜잭션**
- [ ] @Transactional이 메서드 레벨에 붙어 있는가? (클래스 레벨 아님)
- [ ] 조회 메서드에 `readOnly = true`가 붙어 있는가?
- [ ] readOnly Facade에서 변경 Service를 호출하지 않는가?
- [ ] Facade와 Domain Service 양쪽 모두 @Transactional이 있는가?
