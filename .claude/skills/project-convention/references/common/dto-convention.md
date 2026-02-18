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
| **Application** | `~Command` / `~Query` | `~Info` (단일) / `~Result` (조합) | 유스케이스 단위 입출력 |
| **Domain Service** | `~Data` | **Entity** 또는 `~Info` | 외부 도메인 정보 명세. 필요할 때만 생성 |

- Domain Service는 **Entity를 직접 반환하는 게 기본**. `Info`는 Entity 하나로 표현이 안 될 때만 생성한다.
- Application `~Info`는 유스케이스 결과(단일 도메인), `~Result`는 여러 도메인 Info를 조합할 때 사용한다.
- Domain 계층 자체(Entity, VO)는 DTO를 사용하지 않는다.

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
        public ProductCommand.Create toCommand() {
            return new ProductCommand.Create(name, price);
        }
    }

    public record DetailResponse(Long id, String name, int price) {
        public static DetailResponse from(ProductInfo info) {
            return new DetailResponse(info.id(), info.name(), info.price());
        }
    }
}
```

```java
public class ProductCommand {
    public record Create(String name, int price) {}
    public record Update(Long id, String name, int price) {}
}
```

```java
public record ProductInfo(Long id, String name, int price) {
    public static ProductInfo from(Product entity) {
        return new ProductInfo(entity.getId(), entity.getName(), entity.getPrice());
    }
}
```

### 2. 변환 메서드 위치: "아는 쪽"에 둔다

의존 방향(상위 → 하위)을 지키며, **변환 대상을 아는 쪽**에 메서드를 배치한다.

| 변환 | 메서드 위치 | 형태 | 예시 |
|------|-----------|------|------|
| Request → Command | Request | `toCommand()` | `request.toCommand()` |
| Entity → Info | Info | `static from()` | `ProductInfo.from(entity)` |
| Info → Response | Response | `static from()` | `DetailResponse.from(info)` |
| Entity → Data (타 도메인) | Data | `static from()` | `OrderProductData.from(product)` |

> **금지**: 하위 계층이 상위 계층을 아는 것. Domain이 Application DTO를, Application이 Interface DTO를 알면 안 된다.

### 3. Domain Service의 Data / 반환

```java
// 주문 도메인이 상품 도메인에 요구하는 정보 명세
public record OrderProductData(Long productId, String name, Money price, Long shopId) {
    public static OrderProductData from(Product product) {
        return new OrderProductData(
            product.getId(), product.getName(), product.getPrice(), product.getShopId()
        );
    }
}

// 기본: Entity 직접 반환
public Order create(OrderMemberData member, List<OrderProductData> products) {
    return Order.create(member.memberId(), products);
}

// Entity로 표현 불가능할 때만 Info 생성
public record StockDeductionInfo(int remainingStock, boolean success) {}
```

---

## 파라미터 전달 규칙

### 1. Application → Domain Service 입력

파라미터 개수에 따라 전달 방식을 결정한다.

| 파라미터 수 | 전달 방식 | 예시 |
|------------|----------|------|
| **1~3개** | 원시 타입 직접 전달 | `orderService.create(memberId, address, shopId)` |
| **4개 이상** | DTO(`~Data`) 사용 | `orderService.create(orderProductData)` |

> **주의 — VO 전달과 VO 생성은 다르다.**
> Entity 필드용 VO를 호출자(Facade 등)가 **새로 생성하여 전달하는 것은 금지**한다. VO는 Entity 내부에서 원시값으로부터 생성한다 (→ entity-vo-convention 참조).
> 여기서 "직접 전달"이란, Entity getter 등에서 이미 존재하는 값을 꺼내 넘기는 경우를 말한다.

```java
// ✅ 파라미터 3개 이하 → 원시 타입
public Order create(Long memberId, String address, Long shopId) { ... }

// ✅ 파라미터 4개 이상 → Data DTO
public Order create(OrderProductData productData) { ... }
```

### 2. 절대 금지: Domain Service에 타 도메인 객체 노출

Domain Service의 메서드 시그니처에 **타 도메인의 Entity/VO가 직접 나타나면 안 된다.**

```java
// ❌ 절대 금지 - 주문 도메인이 Product 엔티티를 직접 참조
public class OrderService {
    public Order create(Member member, List<Product> products) { ... }
}

// ✅ Data로 변환하여 전달 - 도메인 간 결합 제거
public class OrderService {
    public Order create(OrderMemberData member, List<OrderProductData> products) { ... }
}
```

> Application 계층(Facade)이 타 도메인 Entity → Data 변환을 책임진다.

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

    public OrderDetailResult getDetail(OrderDetailQuery query) {
        OrderInfo order = orderService.getOrder(query.orderId());
        ProductInfo product = productService.getProduct(order.productId());
        MemberInfo member = memberService.getMember(order.memberId());

        return new OrderDetailResult(order, product, member);
    }
}
```

| 상황 | Application 응답 | 사용 시점 |
|------|-----------------|----------|
| 단일 도메인 반환 | `~Info` | 대부분의 경우 |
| 여러 도메인 조합 | `~Result` | 다중 도메인 Info를 합칠 때 |

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
    → ProductCreateCommand             (Application 입력)
      → OrderProductData               (Domain 입력 - 필요 시)
      ← Entity 또는 ~Info              (Domain 출력)
    ← ProductInfo                      (Application 출력 - 단일)
    ← OrderDetailResult                (Application 출력 - 조합)
  ← ProductCreateResponse             (Interface 출력)
```

---

## 체크리스트

- [ ] DTO는 record로 작성했는가?
- [ ] 관련 DTO끼리 Inner Class로 그룹핑했는가?
- [ ] 변환 메서드가 "아는 쪽"에 있는가? (의존 방향 위반 없는가?)
- [ ] Interface DTO에만 `@Valid`, `@JsonProperty` 등이 붙어 있는가?
- [ ] Application DTO(Command/Info)에 API 스펙 관련 어노테이션이 없는가?
- [ ] Domain Service가 Application DTO를 참조하지 않는가?
- [ ] Domain Service의 Info는 Entity로 충분하지 않을 때만 만들었는가?
- [ ] Domain Service 파라미터 1~3개는 원시 타입/VO, 4개+는 Data DTO인가?
- [ ] Domain Service 메서드 시그니처에 타 도메인 Entity가 노출되지 않는가?
- [ ] 여러 도메인 Info 조합 시 Application에서 `~Result`로 합치는가?
