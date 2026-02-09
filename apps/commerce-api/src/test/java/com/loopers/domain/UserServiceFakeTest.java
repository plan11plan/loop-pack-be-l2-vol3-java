package com.loopers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("STEP3: UserService Fake 기반 단위 테스트 (After)")
class UserServiceFakeTest {

    private FakeUserRepository userRepository;
    private FakePasswordEncoder passwordEncoder;
    private UserService userService;

    private String loginId;
    private String rawPassword;
    private String name;
    private String birthDate;
    private String email;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        passwordEncoder = new FakePasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);

        loginId = "testuser1";
        rawPassword = "Test1234!@#";
        name = "홍길동";
        birthDate = "19900115";
        email = "test@example.com";
    }

    private SignupCommand signupCommand() {
        return new SignupCommand(loginId, rawPassword, name, birthDate, email);
    }

    @DisplayName("회원가입")
    @Nested
    class Signup {

        @Test
        @DisplayName("성공 — when-then 0줄, 암호화 결과를 직접 검증")
        void signup_성공() {
            // act
            UserInfo result = userService.signup(signupCommand());

            // assert
            assertThat(result).isNotNull();
            assertThat(result.loginId()).isEqualTo(loginId);

            // 암호화 검증은 repository를 통해 직접 확인
            UserModel saved = userRepository.find(new LoginId(loginId)).orElseThrow();
            assertThat(saved.getPassword().getValue()).isEqualTo("ENCODED_Test1234!@#");
        }

        @Test
        @DisplayName("중복 아이디면 예외")
        void signup_중복아이디_예외() {
            // arrange
            userService.signup(signupCommand());

            // act & assert
            SignupCommand duplicateCommand = new SignupCommand(loginId, "Other123!@#", name, birthDate, email);
            assertThatThrownBy(() ->
                userService.signup(duplicateCommand)
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
            // arrange
            userService.signup(signupCommand());

            // act
            LoginId loginIdVo = new LoginId(loginId);
            userService.changePassword(new ChangePasswordCommand(loginIdVo, rawPassword, "NewPass123!@"));

            // assert
            UserModel updated = userRepository.find(loginIdVo).orElseThrow();
            assertThat(updated.getPassword().getValue()).isEqualTo("ENCODED_NewPass123!@");
        }

        @Test
        @DisplayName("현재 비밀번호 불일치면 예외")
        void changePassword_현재비밀번호_불일치() {
            // arrange
            userService.signup(signupCommand());

            // act & assert
            assertThatThrownBy(() ->
                userService.changePassword(new ChangePasswordCommand(new LoginId(loginId), "Wrong123!@#", "NewPass123!@"))
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
        }

        @Test
        @DisplayName("새 비밀번호가 현재와 같으면 예외")
        void changePassword_새비밀번호가_현재와_같으면_예외() {
            // arrange
            userService.signup(signupCommand());

            // act & assert
            assertThatThrownBy(() ->
                userService.changePassword(new ChangePasswordCommand(new LoginId(loginId), rawPassword, rawPassword))
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("현재 사용 중인 비밀번호는 사용할 수 없습니다.");
        }
    }
}
