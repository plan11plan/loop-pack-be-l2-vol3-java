# 엔티티 / VO 설계 컨벤션

## 목차

1. [Entity 작성 규칙](#1-entity-작성-규칙)
2. [VO 설계 규칙](#2-vo-설계-규칙)
3. [검증 위치 규칙](#3-검증-위치-규칙)
4. [Entity vs Domain Service 로직 배치](#4-entity-vs-domain-service-로직-배치)
5. [체크리스트](#체크리스트)

---

## 1. Entity 작성 규칙

### 기본 구조

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @Embedded
    private Money totalPrice;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // === 생성 === //

    private Order(Long memberId, Money totalPrice, OrderStatus status) {
        this.memberId = memberId;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public static Order create(Long memberId, int price) {
        return new Order(memberId, Money.of(price), OrderStatus.CREATED);
    }

    // === 도메인 로직 === //

    public void cancel() {
        validateCancellable();
        this.status = OrderStatus.CANCELLED;
    }

    // === 검증 === //

    private void validateCancellable() {
        if (this.status != OrderStatus.CREATED) {
            throw new CoreException(OrderErrorCode.ALREADY_CANCELLED);
        }
    }
}
```

### 생성 패턴: 정적 팩토리 메서드 + private 생성자

모든 Entity는 **정적 팩토리 메서드**로 생성한다. 생성자를 직접 노출하지 않는다.
내부 생성은 **private 생성자**를 사용하여 필드를 초기화한다.

```java
// ✅ private 생성자 + 정적 팩토리 메서드 (권장)
private Order(Long memberId, Money totalPrice, OrderStatus status) {
    this.memberId = memberId;
    this.totalPrice = totalPrice;
    this.status = status;
}

public static Order create(Long memberId, int price) {
    return new Order(memberId, Money.of(price), OrderStatus.CREATED);
}

// ❌ 생성자 직접 노출
public Order(Long memberId, int price) { ... }

// ❌ @Builder
@Builder
public Order(Long memberId, int price) { ... }
```

왜 정적 팩토리 + private 생성자인가:
- 생성 의도를 메서드 이름으로 표현할 수 있다 (`create`, `register`, `createFromImport`)
- 생성 시점에 VO 변환, 초기값 설정, 검증을 Entity가 통제한다
- 불변식(invariant)을 생성 시점부터 보장한다
- private 생성자로 필드 초기화가 한 곳에서 완결되어 의도가 명확하다

### 순서 의존성이 있는 생성: 계산 먼저, 생성자에서 완결

하위 엔티티(items 등)로부터 파생되는 필드가 있어도, **Aggregate는 생성 시점부터 완전해야 한다**(DDD Aggregate 불변식). 계산을 생성자 호출 전에 끝내고, 생성자에 모든 값을 넘긴다.

```java
// ❌ 불완전한 객체를 만든 뒤 나중에 채운다
public static OrderModel create(Long userId, List<OrderItemModel> items,
                                int discountAmount) {
    OrderModel order = new OrderModel(userId, OrderStatus.ORDERED); // 가격 정보 없음
    items.forEach(order::addItem);
    order.originalTotalPrice = calculatedPrice;       // 생성 후 직접 할당
    order.discountAmount = discountAmount;             // 생성 후 직접 할당
    order.totalPrice = calculatedPrice - discountAmount; // 생성 후 직접 할당
    return order;
}

// ✅ 계산을 먼저 하고, 생성자에서 모든 필드를 완결한다
public static OrderModel create(Long userId, List<OrderItemModel> items,
                                int discountAmount) {
    validateUserId(userId);
    validateItems(items);
    int originalTotalPrice = OrderItemModel.calculateTotalPrice(items);
    OrderModel order = new OrderModel(
            userId, originalTotalPrice, discountAmount, OrderStatus.ORDERED);
    items.forEach(order::addItem); // JPA 연관관계 설정만 (비즈니스 상태 아님)
    return order;
}

private OrderModel(Long userId, int originalTotalPrice,
                   int discountAmount, OrderStatus status) {
    this.userId = userId;
    this.originalTotalPrice = originalTotalPrice;
    this.discountAmount = discountAmount;
    this.totalPrice = originalTotalPrice - discountAmount;
    this.status = status;
}
```

왜 "계산 먼저, 생성자에서 완결"인가:
- Aggregate는 생성 시점부터 유효한 상태여야 한다 — 불완전한 구간이 존재하면 안 된다
- 생성자에 모든 값이 들어가므로 **불변식을 한 곳에서 보장**할 수 있다
- `addItem`은 JPA 양방향 관계 설정일 뿐, Aggregate의 비즈니스 상태를 바꾸지 않는다

### 접근 제어

| 규칙 | 설정 |
|------|------|
| 기본 생성자 | `@NoArgsConstructor(access = PROTECTED)` — JPA 프록시 전용 |
| Setter | **사용 금지** — 도메인 메서드로 상태 변경 |
| Getter | `@Getter` 허용 — 읽기는 자유 |
| 필드 접근 | `private` — 직접 할당은 Entity 내부에서만 |

```java
// ❌ Setter 금지
order.setStatus(OrderStatus.CANCELLED);

// ✅ 도메인 메서드
order.cancel();
```

### Entity 내부 구조 순서

```java
@Entity
public class Order {
    // 1. 필드 (id, 일반 필드, VO, 연관관계)
    // 2. 정적 팩토리 메서드 (create, register ...)
    // 3. 도메인 로직 메서드 (cancel, changeStatus ...)
    // 4. private 검증 메서드 (validateXxx ...)
}
```

---

## 2. VO 설계 규칙

### 기본 원칙: VO를 만들지 않는다

**모든 검증과 행위는 Entity 도메인 메서드 또는 Domain Service에서 처리한다.** VO를 만들지 않는 이유:

1. **설계**: 검증만 있는 VO는 Entity 메서드로 충분하고, VO 관리 부담이 캡슐화 이득보다 크다
2. **실무**: 필드마다 "이 필드는 VO인가?" 확인하는 인지 비용이 개발 속도를 떨어뜨린다
3. **기술(JPA)**: `@Embeddable`은 같은 타입 2개 사용 시 `@AttributeOverride` 보일러플레이트, 내부 필드 전부 null이면 객체 자체 null 등 기술적 마찰이 있다

| 검증/행위 유형 | VO 생성 | 처리 위치 |
|----------|---------|----------|
| null/길이/범위 검증 | ❌ | Entity 도메인 메서드 (`private static validateXxx`) |
| 형식 규칙 (이메일, 비밀번호 정책) | ❌ | Entity 도메인 메서드 또는 Domain Service |
| 외부 인프라 의존 (암호화 등) | ❌ | Domain Service |
| 도메인 행위 (계산, 변환) | ❌ | Entity 도메인 메서드 |

```java
// Entity에서 직접 검증 + 행위 처리
@Entity
public class ProductModel extends BaseEntity {

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "stock", nullable = false)
    private int stock;

    public static ProductModel create(BrandModel brand, String name, int price, int stock) {
        validatePriceRange(price);
        validateStockRange(stock);
        return new ProductModel(brand, name, price, stock);
    }

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    private static void validatePriceRange(int price) {
        if (price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.");
        }
    }
}
```

### 예외적으로 VO를 만드는 조건

아래 **세 가지를 모두** 충족할 때만 record VO를 만든다:

1. 도메인 행위(계산, 변환, 비교)가 **2개 이상** 존재
2. **여러 도메인**에서 동일 행위가 중복
3. DB 저장과 무관 (**record**로 구현, `@Embeddable` 지양)

```java
// ✅ 예외적 VO — 행위 2개 이상 + 다도메인 중복 + 비저장
public record DateRange(LocalDate start, LocalDate end) {

    public DateRange {
        if (start.isAfter(end)) {
            throw new CoreException(ErrorType.BAD_REQUEST, 
                "시작일이 종료일보다 늦을 수 없습니다.");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
```

record는 불변, equals/hashCode/toString 자동 생성. compact constructor에서 자기 검증.

### 공유 VO 배치 규칙

예외적으로 VO를 만들 경우, 아키텍처에 따라 배치한다:

| 아키텍처 | 배치 위치 |
|----------|----------|
| 레이어 우선 (현재) | `domain.common` 패키지 |
| 도메인 우선 | `common.domain` 패키지 |
| 멀티모듈 | `common-domain` 모듈 |

**주의**: common이 비대해지지 않게 진입 기준을 엄격히 적용한다.

### @Embeddable 개념 그룹핑

Entity 내에서 관련 필드가 하나의 개념 단위를 이룰 때 `@Embeddable`로 그룹핑한다. 행위 중심의 record VO와는 목적이 다르다.

**사용 기준 — 아래 세 가지를 모두 충족할 때:**

1. **3개 이상**의 필드가 하나의 개념을 표현 (예: 스냅샷, 주소, 좌표)
2. 해당 필드들이 항상 **함께 생성**되고 **함께 조회**됨
3. 도메인 성장에 따라 필드가 **늘어날 가능성**이 있음

**규칙:**

- 행위 없이 **데이터 그룹핑만** 담당 (행위가 필요하면 Entity 메서드에서 처리)
- 같은 Entity에 같은 타입 2개 사용 금지 (`@AttributeOverride` 보일러플레이트 방지)
- 클래스명은 `{개념}Snapshot`, `{개념}Info` 등 역할이 드러나는 이름 사용

```java
// ✅ @Embeddable 그룹핑 — 스냅샷 필드 3개 이상, 함께 생성/조회, 확장 가능성
@Embeddable
public class ProductSnapshot {

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    @Column(name = "image_url")
    private String imageUrl;

    protected ProductSnapshot() {
    }

    public ProductSnapshot(String productName, String brandName, String imageUrl) {
        this.productName = productName;
        this.brandName = brandName;
        this.imageUrl = imageUrl;
    }
}
```

---

## 3. 검증 위치 규칙

두 수준으로 나눈다.

| 검증 수준 | 위치 | 기준 |
|----------|------|------|
| **단일 값 / 크로스필드 검증** | Entity `private static validateXxx` 메서드 | 자기 필드만으로 판단 가능 |
| **외부 의존 검증** | Domain Service | Repository 조회, 타 도메인 데이터, 인프라(암호화 등) 필요 |

### 판단 플로우

```
이 검증이 자기 필드만으로 완결되는가?
  ├── YES → Entity의 private static validateXxx 메서드
  └── NO → 뭐가 더 필요한가?
        ├── Repository 조회 → Domain Service
        ├── 타 도메인 정보 → Domain Service
        └── 외부 인프라 (암호화 등) → Domain Service
```

### 예시

```java
// 1. 단일 값 검증 → Entity 내부
@Entity
public class UserModel {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    private static void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }
}

// 2. 외부 의존 검증 → Domain Service
public class UserService {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("...");

    public UserModel signup(String loginId, String rawPassword, ...) {
        validatePasswordFormat(rawPassword);          // 형식 검증도 Service에서 (암호화 전 raw 값 필요)
        validateBirthDateNotInPassword(rawPassword, birthDate); // 크로스필드 + 인프라 의존
        UserModel.create(loginId, passwordEncoder.encode(rawPassword), ...); // 암호화된 값 전달
    }
}
```

---

## 4. Entity vs Domain Service 로직 배치

핵심 기준: **"자기 상태(필드)만으로 완결되는가?"**

### Entity에 둔다

- 자기 상태 변경: `order.cancel()`
- 자기 상태 검증: `order.validateCancellable()`
- 자기 상태로 계산: `order.calculateTotalPrice()`
- 생성 로직: `Order.create(...)`

### Domain Service에 둔다

- Repository 조회 필요: `orderService.findOrThrow(id)`
- 타 도메인 데이터 필요: `orderService.create(memberData, productData)`
- 여러 Entity 조율: `orderService.transferOwnership(from, to)`
- 외부 시스템 연동: `orderService.requestPayment(order)`

### 판단 플로우

```
이 로직이 자기 필드만으로 완결되는가?
  ├── YES → Entity에 둔다
  └── NO → 뭐가 더 필요한가?
        ├── Repository 조회 → Domain Service
        ├── 타 도메인 정보 → Domain Service
        ├── 여러 Entity 조율 → Domain Service
        └── 외부 시스템 → Domain Service
```

### 분리 신호

Entity에 먼저 넣고, 아래 신호가 보이면 Domain Service로 추출한다.

| 신호 | 액션 |
|------|------|
| Entity 메서드가 20개 이상 | 관련 로직 묶어서 Domain Service로 추출 |
| Entity 테스트에 mock이 필요해짐 | 외부 의존이 있다는 뜻 → Domain Service로 |
| 같은 검증 로직이 여러 Entity에 중복 | Domain Service 또는 공통 VO로 추출 |

---

## 체크리스트

**Entity**
- [ ] 정적 팩토리 메서드로 생성하는가?
- [ ] `@NoArgsConstructor(access = PROTECTED)`가 있는가?
- [ ] Setter 없이 도메인 메서드로 상태를 변경하는가?
- [ ] 자기 필드만으로 완결되는 로직만 Entity에 있는가?
- [ ] 필드는 원시값(`int`, `String`, `LocalDate` 등)으로 선언하는가?
- [ ] 각 도메인 메서드에서 필요한 검증을 수행하는가?

**VO (예외적 생성 시)**
- [ ] 도메인 행위가 2개 이상 + 여러 도메인에서 중복되는 경우에만 생성했는가?
- [ ] `record`로 구현했는가? (`@Embeddable` 지양)
- [ ] `domain.common` (또는 아키텍처별 공유 영역)에 배치했는가?

**검증 위치**
- [ ] 자기 필드 검증이 Entity `private static validateXxx` 메서드에 있는가?
- [ ] 외부 의존(인프라, 타 도메인) 검증이 Domain Service에 있는가?

**로직 배치**
- [ ] Repository/타 도메인 필요한 로직이 Domain Service에 있는가?
- [ ] Entity에 외부 의존이 침투하지 않았는가?
