# API 설계 컨벤션

## 목차

1. [URL 구조](#1-url-구조)
2. [HTTP 메서드 규칙](#2-http-메서드-규칙)
3. [HTTP 상태 코드](#3-http-상태-코드)
4. [엔드포인트 설계 패턴](#4-엔드포인트-설계-패턴)
5. [쿼리 파라미터 규칙](#5-쿼리-파라미터-규칙)
6. [Controller 분리 규칙](#6-controller-분리-규칙)
7. [요청/응답 본문 규칙](#7-요청응답-본문-규칙)
8. [체크리스트](#체크리스트)

---

## 1. URL 구조

### API Prefix — 액터별 이중 prefix

| 대상 | Prefix | 예시 |
|------|--------|------|
| 대고객 (Guest/User) | `/api/v1` | `/api/v1/products` |
| 어드민 (Admin) | `/api-admin/v1` | `/api-admin/v1/products` |

같은 도메인이라도 액터별로 prefix가 다르다. 고객용과 Admin용은 인증 방식, 요청/응답 DTO, 비즈니스 정책이 모두 다르기 때문이다.

### 버전 전략 — URL 경로 기반

버전은 **URL 경로**에 포함한다. Header 기반이나 쿼리 파라미터 방식보다 직관적이고, 디버깅/문서화가 쉽다.

```
/api/v1/products          ✅  URL 경로
/api/products?version=1   ❌  쿼리 파라미터
Accept: application/v1    ❌  Header 기반
```

### 리소스 네이밍 — 복수형, 소문자, 케밥케이스

리소스명은 **복수형**을 사용한다. REST에서 리소스는 "컬렉션"을 나타내며, 단건 접근은 `/{id}`로 구분한다.

```
/api/v1/products              ✅  복수형
/api/v1/products/{productId}  ✅  컬렉션 → 단건
/api/v1/product               ❌  단수형
/api/v1/Products              ❌  대문자
```

다중 단어 리소스는 **케밥케이스(kebab-case)**를 사용한다.

```
/api/v1/order-items           ✅  케밥케이스
/api/v1/orderItems            ❌  camelCase
/api/v1/order_items           ❌  snake_case
```

### 경로 변수 네이밍

경로 변수는 **camelCase**로 작성하고, 어떤 리소스의 ID인지 명확히 표현한다.

```
/api/v1/products/{productId}  ✅  리소스명 + Id
/api/v1/products/{id}         ❌  모호한 id
```

---

## 2. HTTP 메서드 규칙

### 메서드별 용도

| Method | 용도 | 멱등성 | 요청 Body |
|--------|------|--------|----------|
| **GET** | 리소스 조회 (목록/단건) | ✅ | 없음 |
| **POST** | 리소스 생성, 비CRUD 행위 | ❌ | 있음 |
| **PUT** | 리소스 전체 수정 | ✅ | 있음 |
| **DELETE** | 리소스 삭제 | ✅ | 없음 |

### PUT만 사용, PATCH 미사용

수정 API는 **PUT**으로 통일한다. 클라이언트가 수정 가능한 필드를 전부 보내는 "전체 교체" 방식이다.

```java
// PUT /api/v1/products/{productId}
// → 클라이언트가 모든 수정 가능 필드를 전송
public record UpdateRequest(
                @NotBlank String name,
                @Positive int price,
                @PositiveOrZero int stock
        ) {}
```

PATCH를 사용하지 않는 이유:
- 현재 도메인의 수정 대상 필드가 적어 부분 수정의 실익이 없다
- 전체 교체 방식이 구현/검증이 단순하다
- null과 "값을 지우겠다"의 구분이 불필요하다

향후 필드가 많아져서 부분 수정이 자연스러운 경우가 생기면, 해당 API에 한해 PATCH를 도입할 수 있다.

### GET에 Body를 넣지 않는다

GET 요청의 필터/정렬/페이지네이션은 **쿼리 파라미터**로 전달한다. GET Body는 일부 인프라에서 무시될 수 있다.

```
GET /api/v1/products?brandId=1&sort=latest&page=0&size=20   ✅
GET /api/v1/products  body: { "brandId": 1 }                ❌
```

---

## 3. HTTP 상태 코드

### 성공 응답

| 상황 | 상태 코드 | 응답 Body |
|------|----------|----------|
| 조회 성공 | **200 OK** | `ApiResponse.success(data)` |
| 생성 성공 | **201 Created** | `ApiResponse.success(data)` |
| 수정 성공 | **200 OK** | `ApiResponse.success(data)` 또는 `ApiResponse.success()` |
| 삭제 성공 | **200 OK** | `ApiResponse.success()` |

생성(POST)만 **201**로 구분한다. 삭제에 204(No Content)를 쓰지 않는 이유는 `ApiResponse` 래퍼를 일관되게 유지하기 위함이다 — 204는 body가 비어야 하므로 `ApiResponse` 포맷과 충돌한다.

```java
// Controller 예시
@PostMapping
public ResponseEntity<ApiResponse<ProductDetailResponse>> create(...) {
    ProductInfo info = productFacade.create(request.toCommand());
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(ProductDetailResponse.from(info)));
}

@DeleteMapping("/{productId}")
public ResponseEntity<ApiResponse<Object>> delete(...) {
    productFacade.delete(productId);
    return ResponseEntity.ok(ApiResponse.success());
}
```

### 에러 응답

에러 응답의 HTTP 상태 코드는 **ErrorCode.getStatus()가 결정**한다. 에러를 200으로 보내지 않는다.

| 상황 | 상태 코드 | 결정 주체 |
|------|----------|----------|
| Validation 실패 | **400** | ControllerAdvice 고정 |
| 비즈니스 에러 | **ErrorCode.getStatus()** | 도메인 ErrorCode enum |
| 서버 에러 | **500** | ControllerAdvice 최후 방어 |

ErrorCode에 정의된 status가 그대로 HTTP 상태 코드가 된다:

```
OrderErrorCode.STOCK_INSUFFICIENT  → HttpStatus.BAD_REQUEST  → 400
ErrorType.NOT_FOUND                → HttpStatus.NOT_FOUND    → 404
BrandErrorCode.DUPLICATE_NAME      → HttpStatus.CONFLICT     → 409
```

### 자주 쓰는 에러 상태 코드

| 상태 코드 | 의미 | 사용 시점 |
|----------|------|----------|
| 400 | Bad Request | Validation 실패, 잘못된 요청 |
| 401 | Unauthorized | 인증 실패 (로그인 필요) |
| 403 | Forbidden | 권한 없음 (본인 리소스 아님) |
| 404 | Not Found | 리소스 없음, soft delete된 리소스 |
| 409 | Conflict | 중복 (브랜드명 중복 등) |
| 500 | Internal Server Error | 예상치 못한 서버 에러 |

---

## 4. 엔드포인트 설계 패턴

### 패턴 ①: 표준 CRUD

대부분의 리소스는 이 패턴을 따른다.

```
GET    /api/v1/{resources}              ← 목록 조회
GET    /api/v1/{resources}/{id}         ← 단건 조회
POST   /api/v1/{resources}              ← 생성
PUT    /api/v1/{resources}/{id}         ← 수정
DELETE /api/v1/{resources}/{id}         ← 삭제
```

```
// 예시: 브랜드 Admin CRUD
GET    /api-admin/v1/brands
GET    /api-admin/v1/brands/{brandId}
POST   /api-admin/v1/brands
PUT    /api-admin/v1/brands/{brandId}
DELETE /api-admin/v1/brands/{brandId}
```

### 패턴 ②: 중첩 리소스 (Nested Resource)

리소스가 상위 리소스에 **종속**될 때 사용한다. "이 상품에 대한 좋아요"처럼 소속 관계가 명확한 경우다.

```
POST   /api/v1/{parent}/{parentId}/{child}
DELETE /api/v1/{parent}/{parentId}/{child}
```

```
// 예시: 상품의 좋아요
POST   /api/v1/products/{productId}/likes     ← 좋아요 등록
DELETE /api/v1/products/{productId}/likes     ← 좋아요 취소
```

중첩 리소스 사용 기준:
- 하위 리소스가 상위 리소스 없이는 의미가 없을 때
- URL만 보고 "무엇에 대한 행위인지" 파악 가능해야 할 때
- 깊이는 **2단계까지만** 허용한다 (`/a/{aId}/b` ✅, `/a/{aId}/b/{bId}/c` ❌)

### 패턴 ③: 소유자 기준 조회

"내 리소스 목록"을 조회할 때, 소유자를 URL에 표현한다.

```
GET /api/v1/{owner}/{ownerId}/{resources}
```

```
// 예시: 내가 좋아요한 상품 목록
GET /api/v1/users/{userId}/likes
```

### 비CRUD 행위 표현

CRUD로 매핑이 어려운 행위는 **리소스 하위에 동사를 붙인다**. 단, 가능하면 리소스 중심으로 먼저 설계하고, 정말 안 될 때만 사용한다.

```
// 리소스로 표현 가능하면 리소스 방식 우선
POST /api/v1/orders                        ✅  주문 "생성"으로 표현

// 리소스로 표현이 어려운 행위
POST /api/v1/products/{productId}/restock   ← 재입고 (향후 예시)
```

---

## 5. 쿼리 파라미터 규칙

### 페이지네이션 — Offset 기반 기본

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `page` | 페이지 번호 (0부터 시작) | `0` |
| `size` | 페이지당 항목 수 | `20` |

```
GET /api-admin/v1/brands?page=0&size=20
GET /api/v1/products?page=1&size=10
```

Spring Data의 `Pageable`과 자연스럽게 연동된다. Cursor 기반 페이지네이션은 무한 스크롤 등 필요한 API에 한해 별도 도입한다.

### 필터링 — 쿼리 파라미터로 전달

필터 조건은 **필드명을 그대로** 쿼리 파라미터명으로 사용한다.

```
GET /api/v1/products?brandId=1
GET /api-admin/v1/products?brandId=1
```

### 정렬 — sort 파라미터

정렬 기준은 `sort` 파라미터로 전달한다. 값은 **snake_case**로 표현한다.

```
GET /api/v1/products?sort=latest           ← 최신순 (기본값)
GET /api/v1/products?sort=price_asc        ← 가격 낮은순
GET /api/v1/products?sort=likes_desc       ← 좋아요 많은순
```

### 날짜 범위 필터

기간 필터는 `startAt`, `endAt` 파라미터를 사용한다.

```
GET /api/v1/orders?startAt=2025-01-01&endAt=2025-01-31
```

### 파라미터 네이밍 규칙

쿼리 파라미터명은 **camelCase**를 사용한다. JSON 필드명과 일관성을 유지한다.

```
?brandId=1&startAt=2025-01-01   ✅  camelCase
?brand_id=1&start_at=2025-01-01 ❌  snake_case
?brand-id=1                     ❌  kebab-case
```

---

## 6. Controller 분리 규칙

### 고객 / Admin Controller 분리

같은 도메인이라도 **고객용과 Admin용 Controller를 분리**한다. 인증 방식, 요청/응답 DTO, prefix가 모두 다르기 때문이다.

```
interfaces/
└── product/
    ├── ProductController.java            ← /api/v1/products (고객)
    ├── AdminProductController.java       ← /api-admin/v1/products (Admin)
    └── dto/
        ├── ProductDto.java               ← 고객용 Request/Response
        └── AdminProductDto.java          ← Admin용 Request/Response
```

### Controller 네이밍

| 대상 | 네이밍 | RequestMapping |
|------|--------|----------------|
| 고객 | `{Domain}Controller` | `@RequestMapping("/api/v1/{resources}")` |
| Admin | `Admin{Domain}Controller` | `@RequestMapping("/api-admin/v1/{resources}")` |

```java
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductFacade productFacade;

    @GetMapping("/{productId}")
    public ApiResponse<ProductDto.DetailResponse> getProduct(...) { ... }
}

@RestController
@RequestMapping("/api-admin/v1/products")
public class AdminProductController {
    private final ProductFacade productFacade;

    @GetMapping("/{productId}")
    public ApiResponse<AdminProductDto.DetailResponse> getProduct(...) { ... }
}
```

### Facade 공유 규칙

고객 Controller와 Admin Controller는 **같은 Facade를 공유할 수 있다**. 단, Admin 전용 로직이 커지면 별도 Facade로 분리한다.

```
// 초기 — Facade 공유
ProductController       → ProductFacade
AdminProductController  → ProductFacade

// Admin 로직이 커지면 — Facade 분리
ProductController       → ProductFacade
AdminProductController  → AdminProductFacade
```

분리 시점: Admin 전용 메서드가 Facade의 절반 이상을 차지하거나, Admin만의 복잡한 유스케이스가 생길 때.

### 도메인 간 API가 겹칠 때

좋아요 목록 조회(`GET /api/v1/users/{userId}/likes`)처럼 URL의 루트 리소스와 실제 도메인이 다른 경우:

```
// 좋아요 도메인이 담당한다 — URL의 "likes"가 핵심 리소스
interfaces/
└── like/
    └── LikeController.java              ← /api/v1/users/{userId}/likes
                                         ← /api/v1/products/{productId}/likes
```

Controller를 어디에 둘지는 **핵심 리소스(행위의 주체)**가 기준이다. 좋아요 등록/취소/조회 모두 Like 도메인의 행위이므로 Like의 interfaces에 둔다.

---

## 7. 요청/응답 본문 규칙

### 생성 요청 → 생성된 리소스 반환

POST로 리소스를 생성하면, **생성된 리소스 정보를 응답에 포함**한다. 클라이언트가 별도 조회 없이 바로 사용할 수 있다.

```json
// POST /api-admin/v1/brands → 201 Created
{
    "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
    "data": { "id": 1, "name": "ACNE STUDIOS" }
}
```

### 수정 요청 → 수정된 리소스 반환 (선택)

PUT 수정 후 변경된 리소스를 반환한다. 반환할 필요가 없으면 `ApiResponse.success()`만 반환해도 된다.

### 삭제 요청 → 빈 data

DELETE 성공 시 `data: null`로 반환한다.

```json
// DELETE /api-admin/v1/brands/{brandId} → 200 OK
{
    "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
    "data": null
}
```

### 목록 응답 구조

목록 조회 시 페이지네이션 메타 정보를 포함한다.

```json
{
    "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
    "data": {
        "content": [ ... ],
        "page": 0,
        "size": 20,
        "totalElements": 58,
        "totalPages": 3
    }
}
```

---

## 체크리스트

**URL 구조**
- [ ] 고객 API는 `/api/v1/`, Admin API는 `/api-admin/v1/` prefix인가?
- [ ] 리소스명이 복수형 소문자인가?
- [ ] 경로 변수가 `{리소스명Id}` 형태의 camelCase인가?
- [ ] 중첩 리소스가 2단계를 초과하지 않는가?

**HTTP 메서드/상태 코드**
- [ ] GET 조회, POST 생성, PUT 수정, DELETE 삭제를 지키는가?
- [ ] GET 요청에 Body가 없는가?
- [ ] 생성 성공은 201, 나머지 성공은 200인가?
- [ ] 에러 응답이 200이 아닌 ErrorCode.getStatus() 기준인가?

**쿼리 파라미터**
- [ ] 필터/정렬/페이지네이션이 쿼리 파라미터로 전달되는가?
- [ ] 파라미터명이 camelCase인가?
- [ ] 페이지네이션이 `page` + `size` 형태인가?

**Controller 분리**
- [ ] 고객/Admin Controller가 분리되어 있는가?
- [ ] Admin Controller 네이밍이 `Admin{Domain}Controller`인가?
- [ ] 고객/Admin DTO가 분리되어 있는가?
- [ ] Controller가 핵심 리소스의 도메인 패키지에 배치되어 있는가?

**요청/응답**
- [ ] 생성 응답에 생성된 리소스 정보가 포함되는가?
- [ ] 삭제 응답이 `ApiResponse.success()`인가?
- [ ] 목록 응답에 페이지네이션 메타 정보가 있는가?
- [ ] 모든 응답이 `ApiResponse` 래퍼로 감싸져 있는가?
