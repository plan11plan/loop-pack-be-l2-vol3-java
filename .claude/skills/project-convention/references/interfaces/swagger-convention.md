# API 문서화 (Swagger) 컨벤션

## 목차

1. [ApiSpec 인터페이스 패턴](#1-apispec-인터페이스-패턴)
2. [패키지 배치와 네이밍](#2-패키지-배치와-네이밍)
3. [어노테이션 규칙](#3-어노테이션-규칙)
4. [Controller 연결](#4-controller-연결)
5. [DTO와 Schema 규칙](#5-dto와-schema-규칙)
6. [에러 응답 문서화](#6-에러-응답-문서화)
7. [체크리스트](#체크리스트)

---

## 1. ApiSpec 인터페이스 패턴

### Swagger 어노테이션을 별도 인터페이스로 분리한다

Controller에 Swagger 어노테이션을 직접 달지 않는다. **ApiSpec 인터페이스**에 문서화 어노테이션을 몰아넣고, Controller가 이를 구현한다.

```
interfaces/
└── user/
    ├── UserV1ApiSpec.java              ← Swagger 어노테이션 (인터페이스)
    ├── UserController.java             ← implements UserV1ApiSpec
    └── dto/
        └── UserV1Dto.java
```

왜 분리하는가:
- **Controller가 깨끗하다** — 비즈니스 흐름(요청 → Facade → 응답)만 보인다. Swagger 어노테이션 10줄이 메서드마다 붙으면 가독성이 급격히 떨어진다
- **문서와 구현이 독립적으로 변경된다** — 문서 설명을 바꿔도 Controller diff가 생기지 않는다
- **리뷰가 분리된다** — API 스펙 리뷰와 구현 리뷰를 따로 할 수 있다

### ApiSpec 구조

```java
@Tag(name = "User V1 API", description = "사용자 API 입니다.")
public interface UserV1ApiSpec {

    @Operation(
        summary = "회원가입",
        description = "새로운 사용자를 등록합니다."
    )
    ApiResponse<UserV1Dto.SignupResponse> signup(
        @RequestBody(description = "회원가입 요청 정보")
        UserV1Dto.SignupRequest request
    );

    @Operation(
        summary = "내 정보 조회",
        description = "인증된 사용자의 정보를 조회합니다. 헤더에 X-Loopers-LoginId와 X-Loopers-LoginPw를 포함해야 합니다."
    )
    ApiResponse<UserV1Dto.MyInfoResponse> getMyInfo(
        @Parameter(description = "로그인 ID", required = true)
        String loginId,
        @Parameter(description = "비밀번호", required = true)
        String password
    );
}
```

핵심 원칙:
- **반환 타입**과 **파라미터 타입**은 Controller와 동일하게 맞춘다
- **메서드명**도 Controller와 동일하게 맞춘다
- ApiSpec에는 **Swagger 어노테이션만** 둔다. Spring MVC 어노테이션(`@GetMapping`, `@PathVariable` 등)은 Controller에만 둔다

---

## 2. 패키지 배치와 네이밍

### 파일 위치

ApiSpec 인터페이스는 **Controller와 같은 패키지**에 둔다.

```
interfaces/
├── user/
│   ├── UserV1ApiSpec.java
│   ├── UserController.java
│   └── dto/
│       └── UserV1Dto.java
│
├── product/
│   ├── ProductV1ApiSpec.java
│   ├── AdminProductV1ApiSpec.java
│   ├── ProductController.java
│   ├── AdminProductController.java
│   └── dto/
│       ├── ProductV1Dto.java
│       └── AdminProductV1Dto.java
│
└── like/
    ├── LikeV1ApiSpec.java
    ├── LikeController.java
    └── dto/
        └── LikeV1Dto.java
```

### 네이밍 규칙

| 대상 | 네이밍 | 예시 |
|------|--------|------|
| 고객 ApiSpec | `{Domain}V1ApiSpec` | `ProductV1ApiSpec` |
| Admin ApiSpec | `Admin{Domain}V1ApiSpec` | `AdminProductV1ApiSpec` |

`V1`을 포함하는 이유:
- API 버전이 URL에 `/api/v1`으로 명시되어 있다
- 향후 V2 API가 추가될 때 `ProductV2ApiSpec`으로 자연스럽게 확장된다
- `@Tag`의 name에도 버전이 들어간다 (`"Product V1 API"`)

### DTO 네이밍과의 연관

ApiSpec의 DTO 이름도 **V1**을 포함한다. 같은 도메인이라도 API 버전별로 요청/응답이 달라질 수 있기 때문이다.

```
dto/
├── UserV1Dto.java          ← V1 API용 Request/Response
└── AdminUserV1Dto.java     ← Admin V1 API용
```

---

## 3. 어노테이션 규칙

### 필수 어노테이션

| 어노테이션 | 위치 | 용도 |
|-----------|------|------|
| `@Tag` | 인터페이스 레벨 | API 그룹 이름과 설명 |
| `@Operation` | 메서드 레벨 | API 요약과 상세 설명 |
| `@Parameter` | 파라미터 레벨 | 경로 변수, 헤더, 쿼리 파라미터 설명 |
| `@RequestBody` | 파라미터 레벨 | 요청 본문 설명 |

### @Tag — 인터페이스 레벨

하나의 ApiSpec 인터페이스에 하나의 `@Tag`를 붙인다.

```java
@Tag(name = "Product V1 API", description = "상품 API 입니다.")
public interface ProductV1ApiSpec { ... }

@Tag(name = "Admin Product V1 API", description = "상품 관리 API 입니다.")
public interface AdminProductV1ApiSpec { ... }
```

Tag name 규칙:
- 형식: `"{Domain} V1 API"` / `"Admin {Domain} V1 API"`
- description: 한글, 간결하게

### @Operation — 메서드 레벨

모든 API 메서드에 `@Operation`을 붙인다.

```java
@Operation(
    summary = "상품 목록 조회",
    description = "브랜드, 정렬 조건으로 상품 목록을 조회합니다. 페이지네이션을 지원합니다."
)
```

| 속성 | 규칙 | 예시 |
|------|------|------|
| `summary` | 한 줄, 동사로 시작 | `"상품 목록 조회"`, `"주문 생성"` |
| `description` | 상세 설명. 인증 요구사항, 특이사항 포함 | `"헤더에 X-Loopers-LoginId를 포함해야 합니다."` |

### @Parameter — 경로 변수, 헤더, 쿼리 파라미터

```java
@Operation(summary = "상품 단건 조회")
ApiResponse<ProductV1Dto.DetailResponse> getProduct(
    @Parameter(description = "상품 ID", required = true, example = "1")
    Long productId
);
```

```java
@Operation(summary = "상품 목록 조회")
ApiResponse<PageResponse<ProductV1Dto.ListResponse>> getProducts(
    @Parameter(description = "브랜드 ID (필터)")
    Long brandId,
    @Parameter(description = "정렬 기준", example = "latest")
    String sort,
    @Parameter(description = "페이지 번호", example = "0")
    int page,
    @Parameter(description = "페이지 크기", example = "20")
    int size
);
```

| 속성 | 사용 시점 |
|------|----------|
| `description` | **항상** 작성 |
| `required` | 필수 파라미터일 때 `true` |
| `example` | ID, 페이지 번호 등 구체적 값이 도움될 때 |
| `hidden` | Swagger UI에서 숨길 파라미터 (내부용 헤더 등) |

### @RequestBody — 요청 본문

```java
@Operation(summary = "상품 등록")
ApiResponse<AdminProductV1Dto.DetailResponse> create(
    @RequestBody(description = "상품 등록 요청 정보")
    AdminProductV1Dto.CreateRequest request
);
```

`io.swagger.v3.oas.annotations.parameters.RequestBody`를 사용한다 (Spring의 `@RequestBody`와 다른 패키지).

---

## 4. Controller 연결

### Controller가 ApiSpec을 implements한다

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @Override
    @PostMapping("/signup")
    public ApiResponse<UserV1Dto.SignupResponse> signup(
        @org.springframework.web.bind.annotation.RequestBody @Valid
        UserV1Dto.SignupRequest request
    ) {
        UserInfo info = userFacade.signup(request.toCommand());
        return ApiResponse.success(UserV1Dto.SignupResponse.from(info));
    }

    @Override
    @GetMapping("/me")
    public ApiResponse<UserV1Dto.MyInfoResponse> getMyInfo(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password
    ) {
        UserInfo info = userFacade.getMyInfo(loginId, password);
        return ApiResponse.success(UserV1Dto.MyInfoResponse.from(info));
    }
}
```

핵심 포인트:
- **Spring MVC 어노테이션**(`@GetMapping`, `@PathVariable`, `@RequestHeader`, `@Valid`)은 **Controller에만** 둔다
- **Swagger 어노테이션**(`@Operation`, `@Parameter`, `@Tag`)은 **ApiSpec에만** 둔다
- `@RequestBody`는 주의: Swagger의 `io.swagger.v3.oas.annotations.parameters.RequestBody`는 ApiSpec에, Spring의 `org.springframework.web.bind.annotation.RequestBody`는 Controller에 각각 사용
- `@Override`를 명시하여 ApiSpec과의 연결을 코드에서 확인한다

### Admin Controller도 동일 패턴

```java
@RestController
@RequestMapping("/api-admin/v1/products")
@RequiredArgsConstructor
public class AdminProductController implements AdminProductV1ApiSpec {

    private final ProductFacade productFacade;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<AdminProductV1Dto.DetailResponse>> create(
        @org.springframework.web.bind.annotation.RequestBody @Valid
        AdminProductV1Dto.CreateRequest request
    ) {
        ProductInfo info = productFacade.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(AdminProductV1Dto.DetailResponse.from(info)));
    }
}
```

---

## 5. DTO와 Schema 규칙

### record DTO는 자동으로 Schema가 생성된다

SpringDoc은 record의 필드를 자동으로 Swagger Schema에 반영한다. 대부분의 경우 `@Schema`를 별도로 붙이지 않아도 된다.

```java
// 이것만으로도 Swagger UI에 필드가 표시된다
public record CreateRequest(
    @NotBlank String name,
    @Positive int price,
    @PositiveOrZero int stock
) {}
```

### @Schema가 필요한 경우

필드명만으로는 의미가 불명확하거나, 예시 값이 필요한 경우에만 `@Schema`를 추가한다.

```java
public record CreateRequest(
    @Schema(description = "상품명", example = "오버사이즈 코트")
    @NotBlank String name,

    @Schema(description = "판매가 (원)", example = "129000")
    @Positive int price,

    @Schema(description = "재고 수량", example = "50")
    @PositiveOrZero int stock,

    @Schema(description = "브랜드 ID", example = "1")
    @NotNull Long brandId
) {}
```

`@Schema` 추가 기준:
- **필드명이 모호한 경우** — `status`, `type` 등 여러 의미를 가질 수 있을 때
- **단위가 중요한 경우** — 가격(원), 무게(g) 등
- **enum이나 특정 형식이 있는 경우** — 날짜 포맷, 정렬 값 등
- **example이 이해를 돕는 경우** — API 테스트 시 Swagger UI에서 바로 사용 가능

### Response에도 동일 기준 적용

```java
public record DetailResponse(
    Long id,
    String name,
    int price,
    @Schema(description = "좋아요 수")
    int likeCount,
    @Schema(description = "생성일시", example = "2025-01-15T10:30:00+09:00")
    ZonedDateTime createdAt
) {
    public static DetailResponse from(ProductInfo info) { ... }
}
```

---

## 6. 에러 응답 문서화

### 공통 에러는 ControllerAdvice 레벨에서 문서화

개별 ApiSpec 메서드마다 에러 응답을 반복하지 않는다. 공통 에러(400, 401, 500 등)는 SpringDoc의 글로벌 설정이나 ControllerAdvice에서 한 번만 정의한다.

### 도메인별 특수 에러만 ApiSpec에 명시

해당 API에서만 발생하는 특수한 에러가 있다면 `@ApiResponse`로 명시할 수 있다.

```java
@Operation(
    summary = "상품 좋아요",
    description = "상품에 좋아요를 등록합니다.",
    responses = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "이미 좋아요한 상품"
        )
    }
)
ApiResponse<Void> like(
    @Parameter(description = "상품 ID", required = true)
    Long productId,
    @Parameter(description = "사용자 ID", required = true)
    String loginId
);
```

단, 모든 에러를 나열하지 않는다. 프론트엔드 개발자가 "이 API에서 이런 에러가 나올 수 있구나"를 알아야 하는 경우에만 추가한다.

---

## 체크리스트

**ApiSpec 인터페이스**
- [ ] 모든 Controller에 대응하는 ApiSpec 인터페이스가 있는가?
- [ ] ApiSpec에 `@Tag`가 붙어 있는가?
- [ ] 모든 API 메서드에 `@Operation(summary, description)`이 있는가?
- [ ] 파라미터에 `@Parameter(description)`이 있는가?
- [ ] 요청 본문에 Swagger `@RequestBody(description)`이 있는가?

**Controller 연결**
- [ ] Controller가 ApiSpec을 `implements`하는가?
- [ ] Controller 메서드에 `@Override`가 명시되어 있는가?
- [ ] Spring MVC 어노테이션은 Controller에만, Swagger 어노테이션은 ApiSpec에만 있는가?
- [ ] Swagger `@RequestBody`와 Spring `@RequestBody`가 혼동되지 않는가?

**네이밍/배치**
- [ ] ApiSpec 네이밍이 `{Domain}V1ApiSpec` / `Admin{Domain}V1ApiSpec`인가?
- [ ] ApiSpec이 Controller와 같은 패키지에 있는가?
- [ ] DTO 네이밍이 `{Domain}V1Dto` / `Admin{Domain}V1Dto`인가?

**DTO Schema**
- [ ] 모호한 필드에 `@Schema(description)`이 추가되어 있는가?
- [ ] `@Schema`를 불필요하게 모든 필드에 붙이지 않았는가?
