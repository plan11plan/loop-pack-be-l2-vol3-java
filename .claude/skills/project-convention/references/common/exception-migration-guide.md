# 예외처리 마이그레이션 가이드

기존 코드에서 수정하거나 추가해야 할 항목 목록.

---

## 1. 신규 생성

### `ErrorCode` 인터페이스
- 위치: `com.loopers.support.error.ErrorCode`

```java
package com.loopers.support.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
```

### `FieldError` record (ApiResponse 내부 또는 별도)
- Validation 에러 필드별 상세용

---

## 2. 기존 코드 수정

### `ErrorType` — `ErrorCode` 인터페이스 구현 추가

```diff
- public enum ErrorType {
+ public enum ErrorType implements ErrorCode {
```

변경 없이 `implements ErrorCode`만 추가하면 기존 필드(`status`, `code`, `message`)가 인터페이스를 이미 만족하므로 다른 수정 불필요.

---

### `CoreException` — `ErrorType` → `ErrorCode`로 변경

```diff
  @Getter
  public class CoreException extends RuntimeException {
-     private final ErrorType errorType;
+     private final ErrorCode errorCode;
      private final String customMessage;

-     public CoreException(ErrorType errorType) {
-         this(errorType, null);
+     public CoreException(ErrorCode errorCode) {
+         this(errorCode, null);
      }

-     public CoreException(ErrorType errorType, String customMessage) {
-         super(customMessage != null ? customMessage : errorType.getMessage());
-         this.errorType = errorType;
+     public CoreException(ErrorCode errorCode, String customMessage) {
+         super(customMessage != null ? customMessage : errorCode.getMessage());
+         this.errorCode = errorCode;
          this.customMessage = customMessage;
      }
  }
```

---

### `ApiResponse` — `failValidation` 메서드 추가

```diff
  public record ApiResponse<T>(Metadata meta, T data) {

+     public record FieldError(String field, Object value, String reason) {}

      // 기존 메서드 유지 ...

+     public static ApiResponse<List<FieldError>> failValidation(
+             String errorCode, String errorMessage, List<FieldError> fieldErrors) {
+         return new ApiResponse<>(Metadata.fail(errorCode, errorMessage), fieldErrors);
+     }
  }
```

---

### `ApiControllerAdvice` — 3곳 수정

#### (1) `handle(CoreException e)` — getter 이름 변경

```diff
  @ExceptionHandler
  public ResponseEntity<ApiResponse<?>> handle(CoreException e) {
      log.warn("CoreException : {}", e.getCustomMessage() != null ? e.getCustomMessage() : e.getMessage(), e);
-     return failureResponse(e.getErrorType(), e.getCustomMessage());
+     return failureResponse(e.getErrorCode(), e.getCustomMessage());
  }
```

#### (2) `handleBadRequest(MethodArgumentNotValidException e)` — 필드별 에러 반환

```diff
  @ExceptionHandler
  public ResponseEntity<ApiResponse<?>> handleBadRequest(MethodArgumentNotValidException e) {
-     FieldError fieldError = e.getBindingResult().getFieldError();
-     String message = fieldError != null ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";
-     return failureResponse(ErrorType.BAD_REQUEST, message);
+     List<ApiResponse.FieldError> fieldErrors = e.getBindingResult()
+         .getFieldErrors()
+         .stream()
+         .map(error -> new ApiResponse.FieldError(
+             error.getField(),
+             error.getRejectedValue(),
+             error.getDefaultMessage()
+         ))
+         .toList();
+
+     return ResponseEntity.badRequest()
+         .body(ApiResponse.failValidation(
+             ErrorType.BAD_REQUEST.getCode(),
+             ErrorType.BAD_REQUEST.getMessage(),
+             fieldErrors
+         ));
  }
```

#### (3) `failureResponse` — `ErrorType` → `ErrorCode`

```diff
- private ResponseEntity<ApiResponse<?>> failureResponse(ErrorType errorType, String errorMessage) {
-     return ResponseEntity.status(errorType.getStatus())
-         .body(ApiResponse.fail(errorType.getCode(), errorMessage != null ? errorMessage : errorType.getMessage()));
+ private ResponseEntity<ApiResponse<?>> failureResponse(ErrorCode errorCode, String errorMessage) {
+     return ResponseEntity.status(errorCode.getStatus())
+         .body(ApiResponse.fail(errorCode.getCode(), errorMessage != null ? errorMessage : errorCode.getMessage()));
  }
```

---

## 3. 도메인별 ErrorCode enum — 필요할 때 추가

예시: `com.loopers.domain.order.OrderErrorCode`

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

## 변경 영향 범위

| 파일 | 변경 유형 | 영향도 |
|------|----------|--------|
| `ErrorCode.java` | **신규** | 없음 |
| `ErrorType.java` | `implements ErrorCode` 추가 | 없음 (기존 호환) |
| `CoreException.java` | 필드 타입 변경 | **기존 `getErrorType()` 호출부 전체** |
| `ApiResponse.java` | `FieldError`, `failValidation` 추가 | 없음 (기존 호환) |
| `ApiControllerAdvice.java` | 3곳 수정 | 해당 파일만 |
| 기존 `throw new CoreException(ErrorType.XXX)` | **변경 불필요** | `ErrorType`이 `ErrorCode` 구현하므로 호환 |
