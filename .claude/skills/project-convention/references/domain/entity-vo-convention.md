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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    // === 생성 === //

    public static Order create(Long memberId, int price, List<OrderLineData> lines) {
        Order order = new Order();
        order.memberId = memberId;
        order.totalPrice = Money.of(price);
        order.status = OrderStatus.CREATED;
        order.orderLines = lines.stream()
            .map(OrderLine::create)
            .toList();
        return order;
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

### 생성 패턴: 정적 팩토리 메서드

모든 Entity는 **정적 팩토리 메서드**로 생성한다. 생성자를 직접 노출하지 않는다.

```java
// ✅ 정적 팩토리 메서드
public static Order create(Long memberId, int price) {
    Order order = new Order();
    order.memberId = memberId;
    order.totalPrice = Money.of(price);
    order.status = OrderStatus.CREATED;
    return order;
}

// ❌ 생성자 직접 노출
public Order(Long memberId, int price) { ... }

// ❌ @Builder
@Builder
public Order(Long memberId, int price) { ... }
```

왜 정적 팩토리인가:
- 생성 의도를 메서드 이름으로 표현할 수 있다 (`create`, `register`, `createFromImport`)
- 생성 시점에 VO 변환, 초기값 설정, 검증을 Entity가 통제한다
- 불변식(invariant)을 생성 시점부터 보장한다

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

### VO 생성 기준

**단순 검증만 필요한 필드는 VO로 만들지 않는다.** 형식 규칙, 도메인 행위, 복합 규칙이 있을 때만 VO를 만든다.

| 검증 유형 | VO 생성 | 처리 위치 |
|----------|---------|----------|
| null 검증 | ❌ | `@NotNull`, Entity 메서드 |
| 길이 검증 | ❌ | `@Size`, Entity 메서드 |
| 범위 검증 (0 이상 등) | ❌ | `@Positive`, Entity 메서드 |
| **형식 규칙** (이메일 정규식, 비밀번호 정책) | ✅ | VO 내부 |
| **도메인 행위** (계산, 변환, 비교) | ✅ | VO 내부 |
| **복합 규칙** (암호화, 포맷팅) | ✅ | VO 내부 |

```java
// ❌ VO 안 만듦 — 단순 검증뿐
String name;          // 길이 제한만 → @Size로 충분
int quantity;         // 0 이상만 → @Positive로 충분
Long memberId;        // 단순 식별자
LocalDateTime createdAt;  // 단순 타임스탬프

// ✅ VO 만듦 — 형식 규칙 또는 행위 존재
Email email;          // 정규식 형식 검증
Money price;          // add(), subtract() 계산 행위
Password password;    // 암호화 로직 + 비밀번호 정책 검증
PhoneNumber phone;    // 형식 검증 + 포맷팅
```

### VO 구현 방식

| 조건 | 구현 방식 | 예시 |
|------|----------|------|
| Entity 필드로 DB에 저장됨 | `@Embeddable` 클래스 | Money, Password, Email |
| DB 저장과 무관 | `record` | DateRange, PriceRange |

#### @Embeddable VO (DB 저장)

```java
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode
public class Money {

    @Column(nullable = false)
    private int amount;

    private Money(int amount) {
        validate(amount);
        this.amount = amount;
    }

    public static Money of(int amount) {
        return new Money(amount);
    }

    public Money add(Money other) {
        return Money.of(this.amount + other.amount);
    }

    private void validate(int amount) {
        if (amount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0 이상이어야 합니다.");
        }
    }
}
```

필수 사항:
- `@NoArgsConstructor(PROTECTED)` — JPA 프록시용
- `@EqualsAndHashCode` — 값 동등성
- 생성자 `private` + 정적 팩토리 `of()` — 생성 통제
- 생성 시 자기 검증 — 유효하지 않은 VO는 존재할 수 없다

#### record VO (비저장)

```java
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

### VO 공통 원칙

**① 생성 시 자기 검증**: VO는 생성되는 순간 유효성이 보장된다.

**② 불변**: 상태 변경이 필요하면 새 VO를 반환한다.

```java
// ✅ 새 VO 반환
public Money add(Money other) {
    return Money.of(this.amount + other.amount);
}

// ❌ 내부 상태 변경
public void add(Money other) {
    this.amount += other.amount;
}
```

**③ 값 동등성**: 내부 값이 같으면 같은 객체. `@Embeddable`은 `@EqualsAndHashCode` 명시, `record`는 자동.

### VO 전달 방식: Entity 내부에서 생성

VO는 **무조건 Entity(Aggregate Root) 내부에서 생성**한다. 바깥에서 원시값을 받아서 Entity가 VO로 변환한다.

```java
// ✅ Entity 내부에서 VO 생성 — 원시값을 받는다
public static Order create(Long memberId, String email, int price) {
    Order order = new Order();
    order.memberId = memberId;
    order.email = Email.of(email);      // 내부에서 VO 생성
    order.price = Money.of(price);      // 내부에서 VO 생성
    order.status = OrderStatus.CREATED;
    return order;
}

// ❌ 바깥에서 VO를 만들어서 전달
public static Order create(Long memberId, Email email, Money price) { ... }
```

왜 Entity 내부에서 생성하는가:
- Entity가 자기 VO의 생성을 완전히 통제한다
- 바깥 계층이 도메인 VO 클래스를 알 필요 없다 (결합도 최소)
- 불변성 보장이 Entity 경계 안에서 완결된다
- 호출 지점은 Domain Service 1~2곳뿐이므로 파라미터가 많아도 실질적 부담이 없다

---

## 3. 검증 위치 규칙

세 수준으로 나눈다.

| 검증 수준 | 위치 | 기준 |
|----------|------|------|
| **단일 값 형식/규칙** | VO 내부 | 그 값 하나만으로 판단 가능 |
| **Entity 내부 크로스필드** | Entity 메서드 | 같은 Entity의 여러 필드 간 관계 |
| **외부 의존 크로스필드** | Domain Service | Repository 조회나 타 도메인 데이터 필요 |

### 판단 플로우

```
이 검증이 단일 값의 형식/규칙인가?
  ├── YES → VO 내부
  └── NO → 같은 Entity의 여러 필드 간 관계인가?
            ├── YES → Entity 메서드
            └── NO → Domain Service
```

### 예시

```java
// 1. 단일 값 → VO 내부
@Embeddable
public class Email {
    private Email(String value) {
        if (!value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
        this.value = value;
    }
}

// 2. Entity 크로스필드 → Entity 메서드
@Entity
public class Promotion {
    private LocalDate startDate;
    private LocalDate endDate;

    public static Promotion create(LocalDate start, LocalDate end) {
        validateDateRange(start, end);
        // ...
    }

    private static void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new CoreException(PromotionErrorCode.INVALID_DATE_RANGE);
        }
    }
}

// 3. 외부 의존 → Domain Service
public class OrderService {
    public Order create(OrderMemberData member, List<OrderProductData> products) {
        validateOrderLimit(member);  // 회원 등급별 주문 한도 — 타 도메인 데이터 필요
        // ...
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
- [ ] VO를 Entity 내부에서 원시값으로부터 생성하는가?

**VO**
- [ ] 단순 검증(null, 길이, 범위)만 있는 필드를 VO로 만들지 않았는가?
- [ ] DB 저장 VO는 `@Embeddable` + `@EqualsAndHashCode`인가?
- [ ] 비저장 VO는 `record`인가?
- [ ] 생성 시 자기 검증이 포함되어 있는가?
- [ ] 상태 변경 시 새 VO를 반환하는가? (불변)

**검증 위치**
- [ ] 단일 값 검증이 VO 내부에 있는가?
- [ ] 크로스필드 검증이 Entity 메서드에 있는가?
- [ ] 외부 의존 검증이 Domain Service에 있는가?

**로직 배치**
- [ ] Repository/타 도메인 필요한 로직이 Domain Service에 있는가?
- [ ] Entity에 외부 의존이 침투하지 않았는가?
