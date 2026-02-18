---
name: project-convention
description: Java Spring 계층형 아키텍처 프로젝트 컨벤션. Controller, Facade, Service, Entity, Repository, DTO, VO, ErrorCode, ApiResponse, ApiSpec, Swagger, QueryDSL, BaseEntity, 테스트 작성 시 참조. 패키지 구조, API 설계, Infrastructure, 예외처리, Admin/고객 분리 규칙 포함.
---

# Project Convention

## 아키텍처 전제

```
Interface(Controller) → Application(Facade) → Domain(Entity + Service) ← Infrastructure
```

---

## Quick Reference

### 공통

**패키지 구조 — 계층 우선 + 도메인 하위**

```
com.loopers/
├── interfaces/                   ← Controller, ApiSpec, Request/Response DTO
│   ├── api/                      ← 공통 (ApiResponse, ControllerAdvice)
│   └── {domain}/                 ← 도메인별 Controller, DTO
├── application/                  ← Facade, Command/Query/Info/Result DTO
│   └── {domain}/
├── domain/                       ← Entity, VO, Service, Repository(I/F), ErrorCode
│   └── {domain}/
├── infrastructure/               ← Repository 구현, JPA, QueryDSL
│   └── {domain}/
└── support/                      ← error, config, util
```

**예외 구조**

```
CoreException → ErrorCode (interface)
                 ├── ErrorType (enum)       ← 공통 (support/error/)
                 └── XxxErrorCode (enum)    ← 도메인별 (domain/{domain}/)
```

**API 응답**

```java
ApiResponse.success(data)                                    // 성공
ApiResponse.fail(code, message)                              // 일반 에러
ApiResponse.failValidation(code, message, fieldErrors)       // Validation 에러
```

**계층별 DTO 네이밍**

| 계층 | 요청 | 응답 |
|------|------|------|
| **Interface** | `~Request` | `~Response` |
| **Application** | `~Command` / `~Query` | `~Info` (단일) / `~Result` (조합) |
| **Domain** | `~Data` | **Entity** 또는 `~Info` |

**테스트**

- **JUnit 5 + AssertJ + Mockito**, `@Nested` 행위별 그룹핑
- 메서드명: `{action}_{condition}`, 내부: arrange / act / assert
- 테스트 더블: Domain → **Fake**, Application → **Mockito**, E2E → **실제 Bean**
- DB 격리: `@AfterEach` + `DatabaseCleanUp.truncateAllTables()`

---

### Interface 계층

**API 설계**

- **Prefix**: 고객 `/api/v1`, Admin `/api-admin/v1`
- **리소스**: 복수형, 소문자, 케밥케이스 (`/api/v1/products`)
- **HTTP 메서드**: GET 조회, POST 생성, PUT 수정(PATCH 미사용), DELETE 삭제
- **상태 코드**: 생성 **201**, 나머지 성공 **200**, 에러는 `ErrorCode.getStatus()`
- **페이지네이션**: Offset 기반 (`page=0&size=20`)
- **엔드포인트**: 표준 CRUD / 중첩 리소스(2단계까지) / 소유자 기준 조회

**Controller 분리**

- 고객 `{Domain}Controller` / Admin `Admin{Domain}Controller`
- Facade 공유 가능, Admin 로직 커지면 분리

**Swagger (ApiSpec 인터페이스)**

- Swagger 어노테이션 → `{Domain}V1ApiSpec` / `Admin{Domain}V1ApiSpec`
- Spring MVC 어노테이션 → Controller에만
- Controller가 ApiSpec을 `implements`, `@Override` 명시
- 필수: `@Tag`(인터페이스), `@Operation`(메서드), `@Parameter`(파라미터)
- `@Schema`: 모호한 필드에만 추가

---

### Application 계층

- **Facade만 사용** (ApplicationService 없음)
- Facade: 유스케이스 조율, 도메인 간 흐름 조합, DTO 변환
- @Transactional: **메서드 레벨**, 조회는 `readOnly = true`
- Facade + Domain Service **양쪽 모두** @Transactional (REQUIRED 전파)
- Facade → 타 도메인 **Service 직접 호출** OK, 타 **Facade 호출 금지**

---

### Domain 계층

**Entity**

- 생성: **정적 팩토리 메서드** (`Order.create(...)`)
- 접근: `@NoArgsConstructor(PROTECTED)`, Setter 금지
- 검증 훅: `guard()` override → `@PrePersist`/`@PreUpdate` 시 호출
- 로직 배치: 자기 필드로 완결 → Entity, 그 외 → Domain Service

**Value Object**

- 생성 기준: 단순 검증만이면 안 만듦. 형식 규칙/행위/복합 규칙이 있을 때만
- 구현: DB 저장 → `@Embeddable`, 비저장 → `record`
- 전달: **Entity 내부에서 원시값으로부터 생성** (바깥에서 VO 전달 금지)
- 검증: 단일값 → VO, 크로스필드 → Entity, 외부의존 → Domain Service

---

### Infrastructure 계층

**Repository 3-클래스 패턴**

- `domain/{domain}/OrderRepository` — 순수 Java 인터페이스 (Spring 의존 없음)
- `infrastructure/{domain}/OrderJpaRepository` — Spring Data JPA
- `infrastructure/{domain}/OrderRepositoryImpl` — `@Repository`, 어댑터

**QueryDSL**

- RepositoryImpl에 `JPAQueryFactory` 주입하여 직접 작성
- 메서드 5개 이상이면 `{Domain}QueryRepository`로 분리

**DB 규칙**

- **FK 미사용**: 같은 도메인 → 객체참조(`NO_CONSTRAINT`), 다른 도메인 → ID 참조
- **@OneToMany 미사용**: ID 참조 + 별도 Repository 조회
- **유니크 제약 사용**: 동시성 중복 방지
- **BaseEntity**: `modules/jpa` 제공. id, createdAt, updatedAt, deletedAt, guard(), delete(), restore()

---

## 상세 참조 가이드

**중요: 코드를 작성하거나 수정하기 전에, 해당 작업과 관련된 아래 reference 파일을 반드시 Read 도구로 읽어라.**
경로는 이 SKILL.md 파일 기준 상대경로이며, 절대경로로 변환하여 읽는다.

### 공통

**패키지 구조** → `references/common/package-convention.md`
- 새 도메인 패키지 생성, 클래스 배치, 의존 방향, 도메인 간 참조

**예외처리 / API 응답** → `references/common/exception-convention.md`
- ErrorCode enum 추가, CoreException throw, ControllerAdvice, ApiResponse, Validation 에러

**기존 코드 마이그레이션** → `references/common/exception-migration-guide.md`
- ErrorType → ErrorCode 전환, CoreException/ApiControllerAdvice/ApiResponse 수정

**DTO** → `references/common/dto-convention.md`
- DTO 신규 생성, 계층 간 전달 객체, 변환 메서드(toCommand, from), record Inner Class

**테스트** → `references/common/test-convention.md`
- 테스트 클래스 생성, 네이밍, 단위/통합/E2E 구분, Fake vs Mockito, DB 정리

### Interface 계층

**API 설계** → `references/interfaces/api-convention.md`
- Controller 생성, URL 설계, HTTP 메서드/상태 코드, Admin/고객 분리, 페이지네이션

**Swagger 문서화** → `references/interfaces/swagger-convention.md`
- ApiSpec 인터페이스 생성, @Operation/@Tag/@Parameter, Controller 연결, @Schema

### Application 계층

**서비스 계층** → `references/application/service-layer-convention.md`
- Facade/Domain Service 생성, 책임 배치, @Transactional, 타 도메인 접근, Facade 분리

### Domain 계층

**Entity / VO** → `references/domain/entity-vo-convention.md`
- Entity 생성, 정적 팩토리, VO 판단/구현, 도메인 로직 배치, 검증 위치

### Infrastructure 계층

**Infrastructure** → `references/infrastructure/infrastructure-convention.md`
- Repository 생성, QueryDSL, BaseEntity/guard(), FK/유니크, Soft delete 필터링
