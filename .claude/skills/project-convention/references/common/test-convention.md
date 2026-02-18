# 테스트 컨벤션

## 목차

1. [프레임워크 및 도구](#1-프레임워크-및-도구)
2. [테스트 피라미드 — 계층별 전략](#2-테스트-피라미드--계층별-전략)
3. [테스트 클래스 구조](#3-테스트-클래스-구조)
4. [네이밍 규칙](#4-네이밍-규칙)
5. [테스트 더블 전략](#5-테스트-더블-전략)
6. [테스트 패키지 배치](#6-테스트-패키지-배치)
7. [DB 정리 전략](#7-db-정리-전략)
8. [체크리스트](#체크리스트)

---

## 1. 프레임워크 및 도구

| 도구 | 용도 |
|------|------|
| JUnit 5 | 테스트 프레임워크 |
| AssertJ | 가독성 높은 검증 (assertThat, assertThatThrownBy) |
| Mockito | 테스트 더블 (mock, stub, verify) |
| @SpringBootTest | 통합 테스트, E2E |
| TestRestTemplate | E2E HTTP 요청 |
| DatabaseCleanUp | 테스트 간 DB 격리 |

---

## 2. 테스트 피라미드 — 계층별 전략

### 단위 테스트 (Unit Test)

| 항목 | 내용 |
|------|------|
| 대상 | Entity, VO, Domain Service |
| 환경 | **Spring 없이 순수 JVM** |
| 테스트 더블 | **Fake 우선**, 필요 시 Mockito |
| 속도 | 빠름 (ms 단위) |
| 비중 | 가장 많이 작성 |

```java
class NameTest {
    @Test
    void createName_whenValidNameProvided() {
        Name name = new Name("홍길동");
        assertThat(name.getValue()).isEqualTo("홍길동");
    }
}
```

### 통합 테스트 (Integration Test)

| 항목 | 내용 |
|------|------|
| 대상 | Service, Facade (여러 컴포넌트 연결 상태) |
| 환경 | `@SpringBootTest`, 실제 Bean, Test DB |
| 테스트 더블 | **실제 Bean 사용** (DB 포함) |
| 속도 | 보통 |
| 비중 | 핵심 비즈니스 흐름 위주 |

```java
@SpringBootTest
class UserServiceIntegrationTest {
    @Autowired UserService userService;
    @Autowired DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }
}
```

### E2E 테스트 (End-to-End Test)

| 항목 | 내용 |
|------|------|
| 대상 | Controller → Service → DB 전체 |
| 환경 | `@SpringBootTest(webEnvironment = RANDOM_PORT)` |
| 도구 | `TestRestTemplate` |
| 속도 | 느림 |
| 비중 | 주요 시나리오만 선별 |

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {
    @Autowired TestRestTemplate testRestTemplate;
    @Autowired DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() { databaseCleanUp.truncateAllTables(); }
}
```

---

## 3. 테스트 클래스 구조

### @Nested로 행위별 그룹핑

테스트 클래스 내부를 `@Nested`로 행위(기능) 단위로 그룹핑한다. 부모 `@DisplayName`에 행위를, 자식에 조건+결과를 작성한다.

```java
class UserModelTest {

    // 공통 픽스처는 @BeforeEach에서 초기화
    @BeforeEach
    void setUp() {
        encoder = new FakePasswordEncoder();
        validLoginId = new LoginId("testuser123");
        // ...
    }

    @DisplayName("유저 모델을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("모든 필드가 주어지면, 정상적으로 생성된다.")
        @Test
        void createUserModel_whenAllDataProvided() {
            // act
            UserModel user = new UserModel(...);

            // assert
            assertAll(
                    () -> assertThat(user.getLoginId()).isEqualTo(validLoginId),
                    () -> assertThat(user.getName()).isEqualTo(validName)
            );
        }

        @DisplayName("로그인 ID가 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenLoginIdIsNull() {
            assertThatThrownBy(() -> new UserModel(null, ...))
                .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("비밀번호를 변경할 때, ")
    @Nested
    class ChangePassword {
        // ...
    }
}
```

### 테스트 메서드 내부 구조: arrange / act / assert

주석으로 세 섹션을 구분한다. 단, arrange가 없으면 생략 가능.

```java
@Test
void createOrder_whenAllDataProvided() {
    // arrange
    Long memberId = 1L;
    int price = 50000;

    // act
    Order order = Order.create(memberId, price);

    // assert
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
}
```

예외 검증처럼 한 줄로 끝나면 주석 없이 작성해도 된다.

```java
@Test
void createName_whenNameIsNull() {
    assertThatThrownBy(() -> new Name(null))
            .isInstanceOf(CoreException.class);
}
```

### 검증: assertAll로 다중 검증 묶기

여러 필드를 한번에 검증할 때 `assertAll`을 사용한다. 첫 번째 실패에서 멈추지 않고 모든 검증 결과를 보여준다.

```java
assertAll(
    () -> assertThat(result.loginId()).isEqualTo(loginId),
    () -> assertThat(result.name()).isEqualTo(name),
    () -> assertThat(result.email()).isEqualTo(email)
);
```

### @ParameterizedTest로 경계값 테스트

같은 로직에 여러 입력을 테스트할 때 사용한다.

```java
@DisplayName("2자 이상 10자 이하의 이름이 주어지면, 정상적으로 생성된다.")
@ParameterizedTest
@ValueSource(strings = {"홍길", "홍길동", "가나다라마바사아자차"})
void createName_whenValidNameProvided(String validNameValue) {
    Name name = new Name(validNameValue);
    assertThat(name.getValue()).isEqualTo(validNameValue);
}
```

---

## 4. 네이밍 규칙

### 테스트 클래스명

| 테스트 유형 | 클래스명 패턴 | 예시 |
|-----------|-----------|------|
| 단위 (Entity/VO) | `{클래스명}Test` | `NameTest`, `OrderTest` |
| 단위 (Domain Service) | `{클래스명}Test` | `OrderServiceTest` |
| 통합 | `{클래스명}IntegrationTest` | `UserServiceIntegrationTest` |
| E2E | `{API명}E2ETest` | `UserV1ApiE2ETest` |

### 테스트 메서드명

**영문 camelCase** + `@DisplayName` 한글 조합.

패턴: `{action}_{condition}`

```java
// @DisplayName이 의도를 전달, 메서드명은 식별용
@DisplayName("로그인 ID가 누락되면 예외가 발생한다.")
@Test
void createUserModel_whenLoginIdIsNull() { ... }

@DisplayName("올바른 현재 비밀번호와 유효한 새 비밀번호가 주어지면 비밀번호가 변경된다.")
@Test
void changePassword_success() { ... }
```

조건이 없는 성공 케이스는 `_{condition}` 대신 `_success` 또는 `_whenAllDataProvided`를 사용한다.

### @DisplayName 규칙

| 위치 | 형식 | 예시 |
|------|------|------|
| `@Nested` 클래스 | `"{행위}할 때, "` | `"유저 모델을 생성할 때, "` |
| `@Test` 메서드 | `"{조건}이면, {결과}한다."` | `"로그인 ID가 누락되면 예외가 발생한다."` |

부모 + 자식을 이어 읽으면 자연스러운 한국어 문장이 된다:
> "유저 모델을 생성할 때, 로그인 ID가 누락되면 예외가 발생한다."

---

## 5. 테스트 더블 전략

### 계층별 테스트 더블 선택

| 테스트 대상 | 더블 전략 | 이유 |
|-----------|---------|------|
| **Entity, VO** | 더블 불필요 (순수 로직) | 외부 의존 없음 |
| **Domain Service** | **Fake 우선** | 실제 동작과 유사, 상태 검증 가능 |
| **Application Facade** | **Mockito mock()** | 여러 Service 조합, Fake 비용 큼 |
| **통합 / E2E** | **실제 Bean** | 연동 검증이 목적 |

### Fake — Domain 단위 테스트의 기본

```java
// 인터페이스를 구현하는 가짜 객체 — 실제처럼 동작
public class FakePasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String rawPassword) {
        return "ENCODED_" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encodedPassword.equals("ENCODED_" + rawPassword);
    }
}
```

Fake를 사용하는 이유:
- encode()와 matches()가 **실제처럼 연동**된다 — mock은 각각 별도로 stub해야 함
- **상태 기반 검증**이 가능하다 — "ENCODED_Test1234!@#"이 실제로 저장되었는지 확인
- 테스트가 **구현 세부사항에 결합하지 않는다** — mock은 어떤 메서드가 호출되는지에 결합

### Mockito — Application 계층 테스트

```java
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock OrderService orderService;
    @Mock ProductService productService;
    @InjectMocks OrderFacade orderFacade;

    @Test
    void createOrder_success() {
        // arrange (stub)
        when(productService.getProduct(1L)).thenReturn(productInfo);
        when(orderService.create(any())).thenReturn(order);

        // act
        OrderDetailResult result = orderFacade.createOrder(command);

        // assert
        assertThat(result).isNotNull();
        verify(orderService).create(any());
    }
}
```

### 테스트 더블 선택 판단 플로우

```
테스트 대상이 외부 의존이 있는가?
  ├── NO → 더블 불필요 (Entity, VO)
  └── YES → 의존이 인터페이스로 분리되어 있는가?
              ├── YES → Fake를 만들 가치가 있는가?
              │         ├── 상태 연동이 중요 → Fake
              │         └── 단순 위임 → Mockito mock()
              └── NO → Mockito mock()
```

### Fake 배치

Fake 클래스는 테스트 소스 내에 배치한다.

```
src/test/java/com/loopers/
├── domain/
│   ├── UserModelTest.java
│   └── FakePasswordEncoder.java    ← 테스트 소스에 배치
└── utils/
    └── DatabaseCleanUp.java
```

---

## 6. 테스트 패키지 배치

테스트 클래스는 **프로덕션 코드와 동일한 패키지 구조**를 따른다.

```
src/test/java/com/loopers/
├── domain/
│   ├── order/
│   │   ├── OrderTest.java                  ← Entity 단위
│   │   └── OrderServiceTest.java           ← Domain Service 단위
│   ├── product/
│   │   └── ProductTest.java
│   └── member/
│       └── ...
├── application/
│   └── order/
│       └── OrderFacadeTest.java            ← Application mock 테스트
├── interfaces/
│   └── order/
│       └── OrderV1ApiE2ETest.java          ← E2E
└── utils/
    ├── DatabaseCleanUp.java
    └── FakePasswordEncoder.java             ← 공통 Fake
```

---

## 7. DB 정리 전략

통합/E2E 테스트에서 테스트 간 격리를 위해 `@AfterEach`에서 DB를 정리한다.

```java
@AfterEach
void tearDown() {
    databaseCleanUp.truncateAllTables();
}
```

왜 `truncate`인가:
- `@Transactional` 롤백은 `RANDOM_PORT` E2E에서 동작하지 않는다 (별도 스레드)
- `deleteAll()`은 외래키 순서를 관리해야 하고 느리다
- `truncate`는 빠르고 auto_increment도 초기화된다

---

## 체크리스트

**구조**
- [ ] 행위별로 `@Nested`로 그룹핑했는가?
- [ ] `@DisplayName`이 부모+자식 이어 읽으면 자연스러운 문장인가?
- [ ] 메서드 내부가 arrange / act / assert 순서인가?
- [ ] 다중 검증 시 `assertAll`을 사용했는가?
- [ ] 경계값 테스트에 `@ParameterizedTest`를 활용했는가?

**네이밍**
- [ ] 테스트 클래스명이 `{클래스}Test` / `IntegrationTest` / `E2ETest` 패턴인가?
- [ ] 메서드명이 `{action}_{condition}` 패턴 영문 camelCase인가?
- [ ] `@DisplayName`이 한글로 의도를 명확히 전달하는가?

**테스트 더블**
- [ ] Domain 단위 테스트에서 Fake를 우선 사용했는가?
- [ ] Application 테스트에서 Mockito mock()을 사용했는가?
- [ ] 통합/E2E에서 실제 Bean을 사용했는가?
- [ ] Fake가 테스트 소스에 배치되어 있는가?

**DB 격리**
- [ ] 통합/E2E 테스트에 `@AfterEach` + `truncateAllTables()`가 있는가?
