# 인라인 변수 컨벤션 (Inline Variable Convention)

> 일회용 지역변수를 제거하고, 객체 생성과 사용을 한 흐름으로 응집시킨다.

## 목차

1. [적용 범위](#적용-범위)
2. [핵심 원칙](#핵심-원칙)
3. [줄바꿈 & 들여쓰기 스타일](#줄바꿈--들여쓰기-스타일)
4. [레이어별 적용 예시](#레이어별-적용-예시)
5. [판단 플로우차트](#판단-플로우차트)
6. [체크리스트](#체크리스트)

---

## 적용 범위

모든 레이어 (Controller, Facade, Service, Domain)

---

## 핵심 원칙

### 1. 일회용 지역변수는 인라인한다

변수가 **생성 직후 단 한 번만 참조**되면 인라인한다.

```java
// ❌ Bad — 일회용 변수가 코드만 늘린다
OrderCriteria.ListByDate criteria = new OrderCriteria.ListByDate(startAt, endAt);
List<OrderResult.OrderSummary> results = orderFacade.getMyOrders(loginUser.id(), criteria);
OrderResponse.ListResponse listResponse = new OrderResponse.ListResponse(
    results.stream().map(OrderResponse.OrderSummary::from).toList());
return ApiResponse.success(listResponse);

// ✅ Good — 흐름이 한눈에 읽힌다
List<OrderResult.OrderSummary> results =
        orderFacade.getMyOrders(
                loginUser.id(),
                new OrderCriteria.ListByDate(startAt, endAt));

return ApiResponse.success(
        new OrderResponse.ListResponse(
                results.stream().map(OrderResponse.OrderSummary::from).toList()));
```

### 2. 객체 생성과 사용을 분리하지 않는다

객체를 만들어 변수에 담고 → 다른 곳에 넘기는 2단계 패턴은 응집도를 떨어뜨린다.

```java
// ❌ Bad — 생성과 사용이 분리되어 응집도 저하
OrderItem orderItem = new OrderItem(product, quantity);
order.add(orderItem);

// ✅ Good — 생성과 사용이 한 표현식
order.add(new OrderItem(product, quantity));
```

```java
// ❌ Bad
Address address = new Address(city, street, zipCode);
member.changeAddress(address);

// ✅ Good
member.changeAddress(new Address(city, street, zipCode));
```

### 3. 변수를 유지하는 경우

다음 조건 중 하나라도 해당하면 변수로 추출한다.

| 조건 | 이유 | 예시 |
|------|------|------|
| **2회 이상 참조** | 중복 호출 방지 | `results`를 응답 변환 + 로깅에 사용 |
| **의미 경계가 달라지는 지점** | 가독성, 디버깅 용이 | Facade/Service 호출 결과 |
| **인라인 시 한 줄이 3단계 이상 중첩** | 가독성 한계 | 아래 예시 참고 |

```java
// 인라인 시 중첩이 너무 깊어지는 경우 → 변수 추출
// ❌ 과도한 인라인
return ApiResponse.success(
        new OrderResponse.DetailResponse(
                OrderResponse.OrderDetail.from(
                        orderFacade.getOrderDetail(
                                loginUser.id(),
                                new OrderCriteria.Detail(orderId)))));

// ✅ 의미 경계에서 끊는다
OrderResult.OrderDetail result =
        orderFacade.getOrderDetail(
                loginUser.id(),
                new OrderCriteria.Detail(orderId));

return ApiResponse.success(
        new OrderResponse.DetailResponse(
                OrderResponse.OrderDetail.from(result)));
```

---

## 줄바꿈 & 들여쓰기 스타일

### Chop-down 스타일 (권장)

메서드 인자가 한 줄에 안 들어가면, **첫 번째 인자부터 줄바꿈** + **8칸(continuation indent)** 적용한다.

```java
// 한 줄에 들어가면 그대로
order.add(new OrderItem(product, quantity));

// 안 들어가면 chop-down
List<OrderResult.OrderSummary> results =
        orderFacade.getMyOrders(
                loginUser.id(),
                new OrderCriteria.ListByDate(startAt, endAt));
```

**왜 이 스타일인가?**

| 스타일 | 문제점 |
|--------|--------|
| 괄호 정렬 (Align to parenthesis) | 메서드명 길이에 따라 들여쓰기가 변동, diff가 지저분함 |
| **Chop-down (권장)** | **일관된 indent, 리네임해도 diff 깔끔** |

### IntelliJ 설정

```
Settings > Editor > Code Style > Java > Wrapping and Braces
├── Method call arguments: Chop down if long
├── Continuation indent: 8
└── Align when multiline: OFF (체크 해제)
```

### 닫는 괄호 위치

연쇄된 닫는 괄호 `))` 는 마지막 인자 뒤에 붙인다 (별도 줄 X).

```java
// ✅ 닫는 괄호는 마지막 인자에 붙인다
return ApiResponse.success(
        new OrderResponse.ListResponse(
                results.stream().map(OrderResponse.OrderSummary::from).toList()));

// ❌ 닫는 괄호를 별도 줄에 내리지 않는다
return ApiResponse.success(
        new OrderResponse.ListResponse(
                results.stream().map(OrderResponse.OrderSummary::from).toList()
        )
);
```

---

## 레이어별 적용 예시

### Controller

```java
@GetMapping
@Override
public ApiResponse<OrderResponse.ListResponse> list(
        @Login LoginUser loginUser,
        @RequestParam ZonedDateTime startAt,
        @RequestParam ZonedDateTime endAt
) {
    List<OrderResult.OrderSummary> results =
            orderFacade.getMyOrders(
                    loginUser.id(),
                    new OrderCriteria.ListByDate(startAt, endAt));

    return ApiResponse.success(
            new OrderResponse.ListResponse(
                    results.stream().map(OrderResponse.OrderSummary::from).toList()));
}
```

### Service / Facade

```java
public OrderResult.OrderDetail getOrderDetail(Long userId, OrderCriteria.Detail criteria) {
    Order order = orderReader.read(criteria.orderId());
    order.validateOwner(userId);

    return OrderResult.OrderDetail.from(order);
}
```

### Domain

```java
// ❌ Bad
public void addItem(Product product, int quantity) {
    OrderItem orderItem = new OrderItem(product, quantity, product.getPrice());
    this.orderItems.add(orderItem);
    this.totalAmount = calculateTotal();
}

// ✅ Good
public void addItem(Product product, int quantity) {
    this.orderItems.add(new OrderItem(product, quantity, product.getPrice()));
    this.totalAmount = calculateTotal();
}
```

---

## 판단 플로우차트

```
변수 선언을 만났다
  └─ 이 변수가 2회 이상 참조되는가?
       ├─ YES → 변수 유지
       └─ NO → 인라인 시 중첩이 3단계 이상인가?
              ├─ YES → 의미 경계에서 변수 추출
              └─ NO → 인라인한다
```

---

## 체크리스트

- [ ] 일회용 지역변수(생성 직후 1회만 참조)를 인라인했는가?
- [ ] 객체 생성과 사용이 한 표현식으로 응집되어 있는가?
- [ ] 2회 이상 참조되는 변수는 변수로 유지했는가?
- [ ] 인라인 시 3단계 이상 중첩이 발생하면 의미 경계에서 변수를 추출했는가?
- [ ] 줄바꿈이 Chop-down 스타일(8칸 continuation indent)을 따르는가?
- [ ] 닫는 괄호 `))` 가 마지막 인자 뒤에 붙어 있는가? (별도 줄 X)
- [ ] 괄호 정렬(Align to parenthesis)을 사용하지 않았는가?
