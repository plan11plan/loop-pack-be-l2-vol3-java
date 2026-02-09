package com.loopers.domain;

// ============================================================
// STEP 3: After — Fake 기반 단위 테스트
//
// STEP 1의 UserServiceMockTest와 동일한 시나리오를 테스트하되,
// Mock 대신 Fake를 사용한다.
//
// 비교 포인트:
//   1. infrastructure import 없음 — 도메인 안에서 테스트가 완결됨
//   2. when-then 0줄 — Fake가 알아서 동작
//   3. 실제 저장된 값을 직접 검증 가능 — verify(save) 대신 find()로 확인
//
// Mockito import도 없다. 순수 JUnit만 사용.
// ============================================================

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("STEP3: UserService Fake 기반 단위 테스트 (After)")
class UserServiceFakeTest {

    // Mockito 없음. 순수 Java 객체로 조립.
    private FakeUserRepository userRepository;
    private FakePasswordEncoder passwordEncoder;
    private UserService userService;

    private LoginId validLoginId;
    private Password validPassword;
    private Name validName;
    private BirthDate validBirthDate;
    private Email validEmail;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        passwordEncoder = new FakePasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);

        validLoginId = new LoginId("testuser1");
        validPassword = Password.of("Test1234!@#");
        validName = new Name("홍길동");
        validBirthDate = new BirthDate(LocalDate.of(1990, 1, 15));
        validEmail = new Email("test@example.com");
    }

    @DisplayName("회원가입")
    @Nested
    class Signup {

        @Test
        @DisplayName("성공 — when-then 0줄, 암호화 결과를 직접 검증")
        void signup_성공() {
            // arrange — when-then 없음

            // act
            UserModel result = userService.signup(
                validLoginId, validPassword, validName, validBirthDate, validEmail
            );

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getLoginId()).isEqualTo(validLoginId);

            // Fake라서 암호화 결과가 예측 가능하다.
            // Mock에서는 내가 지시한 "$2a$10$encodedHash"가 나왔지만,
            // Fake에서는 실제 로직(ENCODED_ + 원본)이 동작한 결과가 나온다.
            assertThat(result.getPassword().getValue()).isEqualTo("ENCODED_Test1234!@#");
        }

        @Test
        @DisplayName("중복 아이디면 예외 — Fake에 이미 데이터가 있으므로 자연스럽게 감지")
        void signup_중복아이디_예외() {
            // arrange — 먼저 가입
            userService.signup(validLoginId, validPassword, validName, validBirthDate, validEmail);
            // FakeUserRepository에 데이터가 저장된 상태.
            // Mock이면 when(find).thenReturn(Optional.of(...))를 써야 했음.

            // act & assert — 같은 ID로 다시 가입
            assertThatThrownBy(() ->
                userService.signup(
                    validLoginId,
                    Password.of("Other123!@#"),
                    validName, validBirthDate, validEmail
                )
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("이미 존재하는 아이디입니다.");
        }
    }

    @DisplayName("비밀번호 변경")
    @Nested
    class ChangePassword {

        @Test
        @DisplayName("성공 — when-then 0줄, 변경된 비밀번호를 직접 검증")
        void changePassword_성공() {
            // arrange — 실제 흐름처럼 먼저 가입
            userService.signup(validLoginId, validPassword, validName, validBirthDate, validEmail);
            // Mock이면 여기서 Mock 설정 5줄이 필요했음.
            // Fake는 signup이 실제로 저장하므로 추가 설정 불필요.

            // act
            Password newPassword = Password.of("NewPass123!@");
            userService.changePassword(validLoginId, validPassword, newPassword);

            // assert — 실제 저장된 값을 직접 확인
            UserModel updated = userRepository.find(validLoginId).orElseThrow();
            assertThat(updated.getPassword().getValue()).isEqualTo("ENCODED_NewPass123!@");
            // Mock에서는 verify(save).save(any())로 "호출됐는가?"만 확인했음.
            // Fake에서는 "어떤 값으로 바뀌었는가?"를 직접 검증한다.
        }

        @Test
        @DisplayName("현재 비밀번호 불일치면 예외 — Fake가 실제로 매칭 실패")
        void changePassword_현재비밀번호_불일치() {
            // arrange
            userService.signup(validLoginId, validPassword, validName, validBirthDate, validEmail);

            // act & assert
            Password wrongCurrent = Password.of("Wrong123!@#");
            Password newPassword = Password.of("NewPass123!@");

            assertThatThrownBy(() ->
                userService.changePassword(validLoginId, wrongCurrent, newPassword)
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
            // Mock이면 when(matches).thenReturn(false)를 써야 했음.
            // Fake는 실제로 "ENCODED_Wrong123!@#" ≠ "ENCODED_Test1234!@#"이므로 자연스럽게 실패.
        }

        @Test
        @DisplayName("새 비밀번호가 현재와 같으면 예외")
        void changePassword_새비밀번호가_현재와_같으면_예외() {
            // arrange
            userService.signup(validLoginId, validPassword, validName, validBirthDate, validEmail);

            // act & assert
            assertThatThrownBy(() ->
                userService.changePassword(validLoginId, validPassword, validPassword)
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("현재 사용 중인 비밀번호는 사용할 수 없습니다.");
            // Fake가 실제로 매칭: "ENCODED_Test1234!@#" == "ENCODED_Test1234!@#" → true → 예외
        }
    }
}
