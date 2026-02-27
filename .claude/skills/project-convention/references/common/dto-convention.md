# DTO 컨벤션

## 목차

1. [계층별 DTO 네이밍](#계층별-dto-네이밍)
2. [DTO 작성 규칙](#dto-작성-규칙)
3. [파라미터 전달 규칙](#파라미터-전달-규칙)
4. [계층 간 흐름](#계층-간-흐름-요약)
5. [체크리스트](#체크리스트)

---

## 계층별 DTO 네이밍

| 계층 | 요청 (입력) | 응답 (출력) | 비고 |
|------|------------|------------|------|
| **Interface** | `~Request` | `~Response` | API 스펙 종속, `@Valid` 부착 |
| **Application** | `~Criteria` | `~Result` | 유스케이스 단위 입출력. Criteria는 Domain의 Command를 참조/조합 가능 |
| **Domain Service** | `~Command` | **Entity** 또는 `~Info` | 자기 도메인 비즈니스 입력 (타 도메인 정보 명세 포함) |

- Domain Service는 **Entity를 직접 반환하는 게 기본**. `Info`는 Entity 하나로 표현이 안 될 때만 생성한다.
- Application `~Criteria`는 유스케이스 입력을 표현하며, 내부에서 Domain Service의 `~Command`를 참조하거나 조합할 수 있다.
- Application `~Result`는 유스케이스 결과를 표현한다 (단일/조합 구분 없이 통합).
- Domain Service `~Command`는 자기 도메인 비즈니스 명령과 타 도메인 정보 명세를 모두 포함한다.
- Domain 계층 자체(Entity)는 DTO를 사용하지 않는다.

---

## DTO 작성 규칙

### 1. Inner Class + Record 활용

DTO는 **record**로 작성하고, 관련 DTO끼리 **Inner Class**로 그룹핑한다.

```java
public class ProductDto {

    public record CreateRequest(
        @NotBlank String name,
        @Positive int price
    ) {
        public ProductCriteria.Create toCreateCriteria() {
            return new ProductCriteria.Create(name, price);
        }
    }

    public record DetailResponse(Long id, String name, int price) {
        public static DetailResponse from(ProductResult result) {
            return new DetailResponse(result.id(), result.name(), result.price());
        }
    }
}
```

```java
public class ProductCriteria {
    public record Create(String name, int price) {
        public ProductCommand.Create toCommand() {
            return new ProductCommand.Create(name, price);
        }
    }
}
```

```java
public class ProductCommand {
    public record Create(String name, int price) {}
    public record Update(Long id, String name, int price) {}
    public record StockDeduction(Long productId, int quantity, int expectedPrice) {}
}
```

```java
public class ProductInfo {
    public record StockDeduction(Long productId, String name, int price, int quantity, Long brandId) {}
}
```

```java
public record ProductResult(Long id, String name, int price) {
    public static ProductResult from(Product entity) {
        return new ProductResult(entity.getId(), entity.getName(), entity.getPrice());
    }
}
```

> **Domain DTO 배치**: `~Command`와 `~Info`는 모두 `domain/{도메인}/dto/` 패키지에 배치한다.
> `{Domain}Command`, `{Domain}Info` 그룹 클래스 아래 Inner Class(record)로 그룹핑한다.

### 2. 변환 메서드 위치: "아는 쪽"에 둔다

의존 방향(상위 → 하위)을 지키며, **변환 대상을 아는 쪽**에 메서드를 배치한다.

| 변환 | 메서드 위치 | 형태 | 예시 |
|------|-----------|------|------|
| Request → Criteria | Request | `toCriteria()` | `request.toCreateCriteria()` |
| Criteria → Command | Criteria | `toCommand()` | `criteria.toCommand()` |
| Entity → Result | Result | `static from()` | `ProductResult.from(entity)` |
| Result → Response | Response | `static from()` | `DetailResponse.from(result)` |
| Entity/Info → Command (타 도메인) | Command | `static from()` | `OrderProductCommand.from(product)` |

> **금지**: 하위 계층이 상위 계층을 아는 것. Domain이 Application DTO를, Application이 Interface DTO를 알면 안 된다.

### 3. Domain Service의 Command / 반환

```java
// 주문 도메인이 상품 도메인에 요구하는 정보 명세
public record OrderProductCommand(Long productId, String name, int price, Long shopId) {
    public static OrderProductCommand from(ProductResult product) {
        return new OrderProductCommand(
            product.id(), product.name(), product.price(), product.shopId()
        );
    }
}

// 기본: Entity 직접 반환
public Order create(OrderMemberCommand member, List<OrderProductCommand> products) {
    return Order.create(member.memberId(), products);
}

// Entity로 표현 불가능할 때만 Info 생성 — {Domain}Info의 Inner Class로 작성
// 위치: domain/{도메인}/dto/{Domain}Info.java
public class ProductInfo {
    public record StockDeduction(Long productId, String name, int price, int quantity, Long brandId) {}
}
```

---

## 파라미터 전달 규칙

### 1. Application → Domain Service 입력

파라미터 개수에 따라 전달 방식을 결정한다.

| 파라미터 수 | 전달 방식 | 예시 |
|------------|----------|------|
| **1~3개** | 원시 타입 직접 전달 | `orderService.create(memberId, address, shopId)` |
| **4개 이상** | DTO(`~Command`) 사용 | `orderService.create(orderProductCommand)` |

> **주의 — Entity 필드는 원시값으로 전달한다.**
> Entity 필드에 대응하는 값은 원시 타입(`int`, `String`, `LocalDate` 등)으로 직접 전달한다 (→ entity-vo-convention 참조).

```java
// ✅ 파라미터 3개 이하 → 원시 타입
public Order create(Long memberId, String address, Long shopId) { ... }

// ✅ 파라미터 4개 이상 → Command DTO
public Order create(OrderProductCommand productCommand) { ... }
```

### 2. 절대 금지: Domain Service에 타 도메인 객체 노출

Domain Service의 메서드 시그니처에 **타 도메인의 Entity/VO가 직접 나타나면 안 된다.**

```java
// ❌ 절대 금지 - 주문 도메인이 Product 엔티티를 직접 참조
public class OrderService {
    public Order create(Member member, List<Product> products) { ... }
}

// ✅ Command로 변환하여 전달 - 도메인 간 결합 제거
public class OrderService {
    public Order create(OrderMemberCommand member, List<OrderProductCommand> products) { ... }
}
```

> Application 계층(Facade)이 타 도메인 Entity/Result → Command 변환을 책임진다.

### 3. 타 도메인 출력 조합: Application에서 `~Result`

여러 도메인의 Info를 합쳐야 할 때, **Application 계층이 `~Result`로 조합**한다.

```java
public record OrderDetailResult(
    OrderInfo order,
    ProductInfo product,
    MemberInfo member
) {}
```

```java
@Service
public class OrderFacade {

    public OrderDetailResult getDetail(OrderCriteria.Detail criteria) {
        OrderInfo order = orderService.getOrder(criteria.orderId());
        ProductInfo product = productService.getProduct(order.productId());
        MemberInfo member = memberService.getMember(order.memberId());

        return new OrderDetailResult(order, product, member);
    }
}
```

### Domain Service 응답 기준

| 상황 | Domain Service 응답 | 사용 시점 |
|------|-------------------|----------|
| Entity로 충분 | **Entity 직접 반환** | 대부분의 경우 |
| Entity로 표현 불가 | `~Info` | 복합 결과가 필요할 때 |

---

## 계층 간 흐름 요약

```
Client
  → ProductCreateRequest              (Interface 입력)
    → ProductCriteria.Create           (Application 입력 — Command 참조 가능)
      → ProductCommand.Create          (Domain 입력)
      ← Entity 또는 ~Info              (Domain 출력)
    ← ProductResult                    (Application 출력)
  ← ProductCreateResponse             (Interface 출력)
```

---

## 체크리스트

- [ ] DTO는 record로 작성했는가?
- [ ] 관련 DTO끼리 Inner Class로 그룹핑했는가?
- [ ] 변환 메서드가 "아는 쪽"에 있는가? (의존 방향 위반 없는가?)
- [ ] Interface DTO에만 `@Valid`, `@JsonProperty` 등이 붙어 있는가?
- [ ] Application DTO(Criteria/Result)에 API 스펙 관련 어노테이션이 없는가?
- [ ] Domain Service가 Application DTO를 참조하지 않는가?
- [ ] Domain Service의 Info는 Entity로 충분하지 않을 때만 만들었는가?
- [ ] Domain Service 파라미터 1~3개는 원시 타입, 4개+는 Command DTO인가?
- [ ] Domain Service 메서드 시그니처에 타 도메인 Entity가 노출되지 않는가?
- [ ] 여러 도메인 Info 조합 시 Application에서 `~Result`로 합치는가?
- [ ] Domain DTO(`~Command`, `~Info`)가 `domain/{도메인}/dto/` 패키지에 있는가?
- [ ] Domain `~Command`는 `{Domain}Command`의 Inner Class로 그룹핑되었는가?
- [ ] Domain `~Info`는 `{Domain}Info`의 Inner Class로 그룹핑되었는가?
