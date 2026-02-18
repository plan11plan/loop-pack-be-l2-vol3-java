# 예외처리 및 API 응답 컨벤션

## 목차

1. [예외 구조](#1-예외-구조)
2. [에러코드 규칙](#2-에러코드-규칙)
3. [API 응답 포맷](#3-api-응답-포맷)
4. [ControllerAdvice 구조](#4-controlleradvice-구조)
5. [예외 흐름](#5-예외-흐름)
6. [패키지 배치](#6-패키지-배치)
7. [도메인별 ErrorCode 추가 가이드](#7-도메인별-errorcode-추가-가이드)
8. [체크리스트](#체크리스트)

---

## 1. 예외 구조

### 클래스 다이어그램

```
CoreException (단일 예외 클래스)
  └─ ErrorCode (interface)
       ├── ErrorType (enum)          ← 공통 에러
       ├── OrderErrorCode (enum)     ← 주문 도메인
       ├── ProductErrorCode (enum)   ← 상품 도메인
       └── ...                       ← 필요 시 추가
```

### ErrorCode 인터페이스

```java
public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
```

### ErrorType — 공통 에러

```java
@Getter
@RequiredArgsConstructor
public enum ErrorType implements ErrorCode {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad Request", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized", "인증에 실패했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Not Found", "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, "Conflict", "이미 존재하는 리소스입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

- 공통 에러의 code는 **HttpStatus reason phrase**를 그대로 사용
- 도메인 에러와 구분: code에 `_`가 포함되면 도메인, 아니면 공통

### CoreException

```java
@Getter
public class CoreException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String customMessage;

    public CoreException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public CoreException(ErrorCode errorCode, String customMessage) {
        super(customMessage != null ? customMessage : errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }
}
```

### 도메인별 ErrorCode

```java
@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "ORDER_001", "이미 취소된 주문입니다."),
    STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "ORDER_002", "재고가 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

---

## 2. 에러코드 규칙

### code 체계

| 구분 | 형식 | 예시 |
|------|------|------|
| 공통 | HttpStatus reason phrase | `"Bad Request"`, `"Not Found"` |
| 도메인 | `{DOMAIN}_{NNN}` | `"ORDER_001"`, `"PRODUCT_002"` |

### 번호 규칙

- 001부터 단순 순번
- enum 선언 순서 = 번호 순서
- 삭제된 번호는 결번 처리 (재사용 금지)

### 사용법

```java
throw new CoreException(ErrorType.NOT_FOUND);                                   // 공통
throw new CoreException(OrderErrorCode.ALREADY_CANCELLED);                      // 도메인
throw new CoreException(OrderErrorCode.STOCK_INSUFFICIENT, "상품 A 재고 부족");   // 커스텀 메시지
```

---

## 3. API 응답 포맷

### ApiResponse 구조

```java
public record ApiResponse<T>(Metadata meta, T data) {

    public record Metadata(Result result, String errorCode, String message) {
        public enum Result { SUCCESS, FAIL }

        public static Metadata success() {
            return new Metadata(Result.SUCCESS, null, null);
        }

        public static Metadata fail(String errorCode, String errorMessage) {
            return new Metadata(Result.FAIL, errorCode, errorMessage);
        }
    }

    public record FieldError(String field, Object value, String reason) {}

    // 성공
    public static ApiResponse<Object> success() {
        return new ApiResponse<>(Metadata.success(), null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Metadata.success(), data);
    }

    // 실패 — 일반 에러
    public static ApiResponse<Object> fail(String errorCode, String errorMessage) {
        return new ApiResponse<>(Metadata.fail(errorCode, errorMessage), null);
    }

    // 실패 — Validation 에러 (필드별 상세)
    public static ApiResponse<List<FieldError>> failValidation(
            String errorCode, String errorMessage, List<FieldError> fieldErrors) {
        return new ApiResponse<>(Metadata.fail(errorCode, errorMessage), fieldErrors);
    }
}
```

### 응답 예시

```json
// 성공 (데이터 없음)
{
  "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
  "data": null
}

// 성공 (데이터 있음)
{
  "meta": { "result": "SUCCESS", "errorCode": null, "message": null },
  "data": { "id": 1, "name": "상품A", "price": 50000 }
}

// 실패 — 비즈니스 에러
{
  "meta": { "result": "FAIL", "errorCode": "ORDER_001", "message": "이미 취소된 주문입니다." },
  "data": null
}

// 실패 — 공통 에러
{
  "meta": { "result": "FAIL", "errorCode": "Not Found", "message": "존재하지 않는 요청입니다." },
  "data": null
}

// 실패 — Validation 에러
{
  "meta": { "result": "FAIL", "errorCode": "Bad Request", "message": "잘못된 요청입니다." },
  "data": [
    { "field": "price", "value": -1000, "reason": "0보다 커야 합니다" },
    { "field": "name", "value": "", "reason": "공백일 수 없습니다" }
  ]
}
```

---

## 4. ControllerAdvice 구조

### 처리 우선순위

| 예외 타입 | 성격 | 응답 형태 |
|----------|------|----------|
| `CoreException` | 비즈니스/도메인 에러 | `meta` + `data: null` |
| `MethodArgumentNotValidException` | @Valid 검증 실패 | `meta` + `data: FieldError[]` |
| `MethodArgumentTypeMismatchException` | 파라미터 타입 불일치 | `meta` + `data: null` |
| `MissingServletRequestParameterException` | 필수 파라미터 누락 | `meta` + `data: null` |
| `HttpMessageNotReadableException` | JSON 파싱 실패 (세분화) | `meta` + `data: null` |
| `NoResourceFoundException` | 존재하지 않는 리소스 | `meta` + `data: null` |
| `Throwable` | 예상 못한 에러 (최후 방어) | `meta` + `data: null` |

### 핵심 원칙

- `@RestControllerAdvice` **하나**로 모든 예외 일괄 처리
- `CoreException` 핸들러에서 `ErrorCode` 인터페이스로 공통/도메인 에러 통합 처리
- `HttpMessageNotReadableException`은 `InvalidFormatException`, `MismatchedInputException`, `JsonMappingException`까지 세분화
- `Throwable` 최후 방어 핸들러 필수
- 예외는 발생 지점에서 그대로 전파, Application에서 잡아서 변환하지 않는다

---

## 5. 예외 흐름

```
Domain에서 throw new CoreException(OrderErrorCode.STOCK_INSUFFICIENT)
  → Application 통과 (잡지 않음)
    → Controller 통과
      → @RestControllerAdvice에서 catch
        → ErrorCode에서 status, code, message 추출
          → ApiResponse.fail(code, message) 반환

@Valid 실패 시
  → Controller 진입 전 MethodArgumentNotValidException 발생
    → @RestControllerAdvice에서 catch
      → FieldError 목록 추출
        → ApiResponse.failValidation(code, message, fieldErrors) 반환
```

---

## 6. 패키지 배치

```
support/
└── error/
    ├── ErrorCode.java              ← 인터페이스
    ├── ErrorType.java              ← 공통 에러 enum
    └── CoreException.java          ← 단일 예외 클래스

domain/
├── order/
│   └── OrderErrorCode.java         ← 도메인 패키지 내 배치
└── product/
    └── ProductErrorCode.java

interfaces/
└── api/
    ├── ApiResponse.java            ← 공통 응답 포맷
    └── ApiControllerAdvice.java    ← 글로벌 예외 핸들러
```

---

## 7. 도메인별 ErrorCode 추가 가이드

1. `{Domain}ErrorCode` enum 생성, `ErrorCode` 인터페이스 구현
2. code는 `{DOMAIN}_{001}`부터 순번 부여
3. 도메인 패키지 내 배치
4. 사용: `throw new CoreException({Domain}ErrorCode.XXX)`

---

## 체크리스트

**예외 설계**
- [ ] 새 에러가 공통(`ErrorType`)인가 도메인(`XxxErrorCode`)인가 판단했는가?
- [ ] 도메인 에러코드 번호가 기존 순번 다음인가? (결번 재사용 금지)
- [ ] `CoreException`에 `ErrorCode` 인터페이스 구현체를 넘기고 있는가?

**예외 흐름**
- [ ] Domain 예외를 Application에서 잡아서 변환하고 있지 않은가? (그대로 전파)
- [ ] ControllerAdvice에 `Throwable` 최후 방어 핸들러가 있는가?

**API 응답**
- [ ] 성공 응답이 `ApiResponse.success()` 또는 `ApiResponse.success(data)`를 사용하는가?
- [ ] Validation 에러는 `failValidation`으로 `FieldError[]`를 반환하는가?
- [ ] 일반 에러는 `fail(code, message)`로 `data: null`을 반환하는가?
