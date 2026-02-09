# 패스워드 설계 결정 기록

## 1. Password → EncryptedPassword 리네이밍

### 결정
`Password` VO를 `EncryptedPassword`로 변경

### 이유
- Password라는 이름은 원문(raw)인지 암호화된 값인지 모호함
- 이 객체는 **항상 암호화된 값만** 보관하므로, 이름이 그 사실을 표현해야 함
- `EncryptedPassword`라는 이름 자체가 "원문이 들어올 수 없다"는 불변식을 전달

---

## 2. PasswordEncoder를 도메인 인터페이스로 분리 (DIP)

### 결정
`PasswordEncoder` 인터페이스를 `com.loopers.domain` 패키지에 정의하고, 구현체(`BCryptPasswordEncoderImpl`)는 `infrastructure`에 배치

### 이유
- 도메인 객체가 암호화 기능을 사용하되, 구체적 알고리즘(BCrypt)에는 의존하지 않아야 함
- 테스트 시 `FakePasswordEncoder`로 대체 가능 → 단위 테스트에서 Spring 컨텍스트 불필요
- DIP(의존성 역전 원칙) 적용: 도메인이 인터페이스를 소유하고, 인프라가 구현

---

## 3. EncryptedPassword에 PasswordEncoder를 생성자 주입하지 않는 이유

### 결정
`PasswordEncoder`를 `EncryptedPassword`의 필드가 아닌 **메서드 파라미터**로 전달

### 이유
- EncryptedPassword는 `@Embeddable` (JPA 값 객체)
- JPA가 DB에서 로딩할 때 `protected EncryptedPassword() {}`로 생성 → PasswordEncoder 주입 불가
- DB에서 로딩된 객체에 encoder가 null이면 `matches()` 호출 시 NPE
- 값 객체는 인프라 의존성(상태)을 갖지 않는 것이 원칙
- 메서드 파라미터는 **행위 협력**이지 **상태 의존**이 아님 → VO 불변성 유지

---

## 4. EncryptedPassword.of()를 하나로 통일

### 변경 전
```java
of(String rawPassword, PasswordEncoder encoder)                     // birthDate 검증 없음
of(String rawPassword, PasswordEncoder encoder, BirthDate birthDate) // birthDate 검증 있음
```

### 변경 후
```java
of(String rawPassword, PasswordEncoder encoder)  // 형식 검증 + 암호화만
```

### 이유
- "비밀번호에 생년월일 포함 불가"는 **cross-field validation** (password + birthDate 두 값 필요)
- EncryptedPassword는 비밀번호 하나만 표현하는 값 객체 → 다른 값 객체(BirthDate)를 알 필요 없음
- 테스트를 위해 오버로드를 만드는 것은 설계 신호: **더 작은 단위로 쪼개지 못한 것**
- EncryptedPassword의 책임: 형식 검증 + 암호화 (자기 자신의 관심사만)

---

## 5. 생년월일-비밀번호 교차 검증을 UserModel 생성자로 이동

### 변경 전
- 생성 시: `EncryptedPassword.of(raw, encoder, birthDate)` — VO에서 검증
- 변경 시: `UserModel.changePassword()` — 엔티티에서 검증
- **같은 규칙인데 검증 위치가 달랐음**

### 변경 후
- 생성 시: `UserModel` 생성자에서 검증
- 변경 시: `UserModel.changePassword()`에서 검증
- **두 경로 모두 UserModel이 검증 → 일관성 확보**

### UserModel 생성자
```java
public UserModel(LoginId loginId, String rawPassword, PasswordEncoder encoder,
                 Name name, BirthDate birthDate, Email email) {
    // null 검증
    validateBirthDateNotInPassword(rawPassword, birthDate);
    this.password = EncryptedPassword.of(rawPassword, encoder);
    // ...
}
```

### 이유
- UserModel은 password와 birthDate **두 값을 모두 아는 aggregate**
- cross-field 검증은 두 값을 모두 아는 곳이 담당해야 함
- `changePassword()`가 이미 같은 패턴(rawPassword + encoder를 받아 내부 검증)을 사용 → 생성자도 동일하게 맞추면 일관성 완성
- **불변식 보장**: birthDate 포함 비밀번호를 가진 UserModel은 존재 자체가 불가능

### DDD 관점에서 문제 없는 이유
- 생성자가 받는 `PasswordEncoder`는 `com.loopers.domain`의 **도메인 인터페이스**
- 인프라 구현체(BCrypt)가 아닌 추상에 의존 → DIP 적용 상태
- `changePassword()`도 이미 같은 방식으로 encoder를 받고 있으므로 새로운 의존이 아님

---

## 최종 책임 분배

| 검증/연산 | 위치 | 이유 |
|-----------|------|------|
| 비밀번호 형식 검증 (8~16자, 대소문자, 숫자, 특수문자) | `EncryptedPassword.of()` | 비밀번호 자체의 관심사 |
| 비밀번호에 생년월일 포함 불가 | `UserModel` 생성자 / `changePassword()` | cross-field validation, aggregate가 담당 |
| 암호화 (encode) | `EncryptedPassword.of()` | 형식 검증과 암호화는 원자적으로 수행 |
| 매칭 (matches) | `EncryptedPassword.matches()` | 자기 자신의 값과 비교 |
| 동일 비밀번호 거부 | `UserModel.changePassword()` | 변경 시 비즈니스 규칙 |
