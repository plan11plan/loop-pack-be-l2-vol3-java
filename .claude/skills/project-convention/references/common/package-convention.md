# 패키지 구조 컨벤션

## 목차

1. [전체 구조](#1-전체-구조)
2. [계층별 패키지 상세](#2-계층별-패키지-상세)
3. [공통 패키지 상세](#3-공통-패키지-상세)
4. [계층별 클래스 배치 규칙](#4-계층별-클래스-배치-규칙)
5. [의존 방향 규칙](#5-의존-방향-규칙)
6. [새 도메인 추가 가이드](#6-새-도메인-추가-가이드)
7. [체크리스트](#체크리스트)

---

## 1. 전체 구조

**계층 우선 + 도메인 하위** 방식을 사용한다. 최상위는 계층(interfaces/application/domain/infrastructure)으로 나누고, 각 계층 안에서 도메인별로 분리한다.

```
com.loopers/
│
├── interfaces/                     ← Interface 계층
│   ├── api/                        ← 공통 (ApiResponse, ControllerAdvice)
│   ├── order/                      ← 주문 Controller, Request/Response DTO
│   ├── product/                    ← 상품 Controller, Request/Response DTO
│   └── like/                       ← 좋아요 Controller, Request/Response DTO
│
├── application/                    ← Application 계층
│   ├── order/                      ← 주문 Facade, Command/Query/Info/Result DTO
│   ├── product/
│   └── like/
│
├── domain/                         ← Domain 계층
│   ├── order/                      ← 주문 Entity, VO, Service, Repository(I/F), ErrorCode
│   ├── product/
│   └── like/
│
├── infrastructure/                 ← Infrastructure 계층
│   ├── order/                      ← 주문 Repository 구현, JPA
│   ├── product/
│   └── like/
│
└── support/                        ← 공통 지원 (에러, 설정, 유틸)
    ├── error/
    ├── config/
    └── util/
```

왜 계층 우선인가:
- 계층 간 의존 방향이 패키지 레벨에서 시각적으로 명확하다
- 같은 계층의 클래스를 한 곳에서 파악할 수 있다 (모든 Controller가 interfaces/ 아래)
- 계층별 공통 패턴이나 Base 클래스를 자연스럽게 배치할 수 있다

---

## 2. 계층별 패키지 상세

### interfaces/ — 풀 구조 (대규모 도메인)

```
interfaces/
├── api/                                    ← 공통
│   ├── ApiResponse.java
│   └── ApiControllerAdvice.java
│
└── order/                                  ← 도메인별
    ├── OrderController.java                ← 고객용 Controller
    ├── AdminOrderController.java           ← Admin용 Controller
    └── dto/
        ├── OrderDto.java                   ← Inner Class: CreateRequest, DetailResponse ...
        └── AdminOrderDto.java              ← Admin용 Request/Response
```

### application/ — 풀 구조

```
application/
└── order/
    ├── OrderFacade.java
    └── dto/
        ├── OrderCommand.java               ← Inner Class: Create, Update ...
        ├── OrderQuery.java                 ← Inner Class: Detail, Search ...
        ├── OrderInfo.java                  ← 단일 도메인 응답
        └── OrderDetailResult.java          ← 다중 도메인 조합 응답 (필요 시)
```

### domain/ — 풀 구조

```
domain/
└── order/
    ├── Order.java                          ← Entity (Aggregate Root)
    ├── OrderLine.java                      ← Entity (하위)
    ├── OrderStatus.java                    ← enum / VO
    ├── OrderService.java                   ← Domain Service
    ├── OrderRepository.java                ← Repository 인터페이스
    ├── OrderErrorCode.java                 ← 도메인 에러코드
    └── dto/                                ← 도메인 DTO (필요 시)
        ├── OrderProductData.java           ← 타 도메인 정보 명세
        └── OrderMemberData.java
```

### infrastructure/ — 풀 구조

```
infrastructure/
└── order/
    ├── OrderRepositoryImpl.java            ← Repository 구현체
    └── OrderJpaRepository.java             ← Spring Data JPA 인터페이스
```

### 간소 구조 (소규모 도메인)

빈 계층 패키지는 만들지 않는다. 필요해지면 그때 추가한다.

```
interfaces/
└── wishlist/
    ├── WishlistController.java
    └── dto/
        └── WishlistDto.java

application/
└── wishlist/
    ├── WishlistFacade.java
    └── dto/
        └── WishlistInfo.java

domain/
└── wishlist/
    ├── Wishlist.java
    ├── WishlistService.java
    └── WishlistRepository.java
```

infrastructure가 JpaRepository 하나뿐이면 domain 패키지에 인터페이스만 두고 Spring Data JPA가 자동 구현하도록 한다.

---

## 3. 공통 패키지 상세

### 공통 인터페이스

도메인에 속하지 않는 API 레벨 공통 클래스.

```
interfaces/
└── api/
    ├── ApiResponse.java             ← 공통 응답 포맷
    └── ApiControllerAdvice.java     ← 글로벌 예외 핸들러
```

### support

도메인 로직이 아닌 **기술 지원** 클래스.

```
support/
├── error/
│   ├── ErrorCode.java               ← 인터페이스
│   ├── ErrorType.java               ← 공통 에러 enum
│   └── CoreException.java           ← 단일 예외 클래스
├── config/
│   ├── WebMvcConfig.java
│   ├── SecurityConfig.java
│   └── JpaConfig.java
└── util/                            ← 필요 시만 생성
    └── DateUtils.java
```

support에 넣으면 안 되는 것:
- 도메인 로직이 포함된 클래스 → 해당 도메인 패키지로
- 특정 도메인에만 쓰이는 유틸 → 해당 도메인 패키지로

---

## 4. 계층별 클래스 배치 규칙

### interfaces/{domain}/

| 클래스 | 네이밍 | 예시 |
|--------|--------|------|
| Controller (고객) | `{Domain}Controller` | `OrderController` |
| Controller (Admin) | `Admin{Domain}Controller` | `AdminOrderController` |
| Request DTO | `{Domain}Dto.{Action}Request` | `OrderDto.CreateRequest` |
| Response DTO | `{Domain}Dto.{Action}Response` | `OrderDto.DetailResponse` |
| Admin DTO | `Admin{Domain}Dto.{Action}Response` | `AdminOrderDto.DetailResponse` |

### application/{domain}/

| 클래스 | 네이밍 | 예시 |
|--------|--------|------|
| Facade | `{Domain}Facade` | `OrderFacade` |
| Command DTO | `{Domain}Command.{Action}` | `OrderCommand.Create` |
| Query DTO | `{Domain}Query.{Action}` | `OrderQuery.Search` |
| Info DTO | `{Domain}Info` | `OrderInfo` |
| Result DTO | `{Domain}{Detail}Result` | `OrderDetailResult` |

### domain/{domain}/

| 클래스 | 네이밍 | 예시 |
|--------|--------|------|
| Entity | `{Domain}` | `Order` |
| Domain Service | `{Domain}Service` | `OrderService` |
| Repository (인터페이스) | `{Domain}Repository` | `OrderRepository` |
| ErrorCode | `{Domain}ErrorCode` | `OrderErrorCode` |
| VO / enum | 의미에 맞게 | `OrderStatus`, `Money` |
| Data DTO | `{Target}Data` | `OrderProductData` |

### infrastructure/{domain}/

| 클래스 | 네이밍 | 예시 |
|--------|--------|------|
| Repository 구현체 | `{Domain}RepositoryImpl` | `OrderRepositoryImpl` |
| JPA Repository | `{Domain}JpaRepository` | `OrderJpaRepository` |
| 외부 API 클라이언트 | `{External}Client` | `PaymentClient` |

---

## 5. 의존 방향 규칙

```
interfaces → application → domain ← infrastructure
```

- **interfaces**는 application을 알 수 있다. domain을 직접 참조하지 않는다.
- **application**은 domain을 알 수 있다. interfaces를 알면 안 된다.
- **domain**은 아무 계층도 알지 못한다. 단, domain Service는 `@Service`, `@Transactional`, `Page`/`Pageable` 사용을 허용한다 (service-layer-convention.md § 3~4).
- **infrastructure**는 domain을 알 수 있다 (Repository 인터페이스 구현).

### 도메인 간 의존

- 도메인 간 **Entity 직접 참조 금지**
- Application 계층(Facade)에서 타 도메인의 Domain Service를 호출하여 조합한다
- 필요 시 Domain의 Data DTO로 정보를 전달한다

```java
// ✅ Application에서 타 도메인 Service 호출
// application/order/OrderFacade.java
@Service
public class OrderFacade {
    private final ProductService productService;  // domain/product/
    private final OrderService orderService;      // domain/order/
}

// ❌ Domain에서 타 도메인 직접 참조
// domain/order/OrderService.java
public class OrderService {
    private final ProductRepository productRepository;  // 금지
}
```

---

## 6. 새 도메인 추가 가이드

1. `domain/{domain}/` 패키지부터 시작 (Entity, Repository 인터페이스)
2. API가 필요하면 `interfaces/{domain}/`, `application/{domain}/` 추가
3. 커스텀 Repository 구현이 필요하면 `infrastructure/{domain}/` 추가
4. 도메인 에러가 필요하면 `domain/{domain}/{Domain}ErrorCode.java` 추가
5. 빈 계층은 만들지 않는다 — 필요할 때 추가

---

## 체크리스트

**구조**
- [ ] 최상위가 계층(interfaces/application/domain/infrastructure)으로 나뉘어 있는가?
- [ ] 각 계층 안에서 도메인별로 패키지가 분리되어 있는가?
- [ ] 빈 계층 패키지가 없는가? (불필요한 빈 폴더 금지)
- [ ] 공통 클래스가 interfaces/api/ 또는 support/ 아래에 있는가?

**의존 방향**
- [ ] interfaces → application → domain ← infrastructure 방향을 지키는가?
- [ ] domain Entity/VO/Repository 인터페이스에 Spring 어노테이션(`@Component`, `@Repository`)이 없는가?
- [ ] domain Service의 `@Service`, `@Transactional`, `Page`/`Pageable` 사용은 컨벤션 허용 (service-layer-convention.md § 3~4)
- [ ] 도메인 간 Entity 직접 참조가 없는가?

**네이밍**
- [ ] Controller, Facade, Service, Repository 네이밍이 규칙을 따르는가?
- [ ] DTO가 해당 계층의 dto/ 패키지에 있는가?
- [ ] ErrorCode가 domain/{domain}/ 패키지에 있는가?
