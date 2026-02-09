package com.loopers.domain;

// ============================================================
// STEP 1: Before — Mock 기반 단위 테스트
//
// 이 테스트는 현재 구조의 "불편함"을 기록하기 위해 작성되었다.
// 주목할 점:
//   1. com.loopers.infrastructure.PasswordEncoder를 import하고 있다 (도메인 테스트인데)
//   2. when-then Mock 설정이 테스트마다 3~5줄씩 필요하다
//   3. verify(save)로만 검증 가능 — "어떤 값으로 바뀌었는가?"는 확인 불가
//
// 이 테스트는 STEP 2 리팩토링 후에도 유지되며,
// STEP 3의 Fake 기반 테스트와 비교 대상이 된다.
// ============================================================

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// STEP2 이후: import 불필요 — PasswordEncoder가 같은 domain 패키지로 이동됨
// (STEP1에서는 여기에 com.loopers.infrastructure.PasswordEncoder import가 있었음)

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("STEP1: UserService Mock 기반 단위 테스트 (Before)")
@ExtendWith(MockitoExtension.class)
class UserServiceMockTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    // [불편함 1] 이 Mock의 타입이 com.loopers.infrastructure.PasswordEncoder이다.

    @InjectMocks
    private UserService userService;

    private LoginId validLoginId;
    private Password validPassword;
    private Name validName;
    private BirthDate validBirthDate;
    private Email validEmail;

    @BeforeEach
    void setUp() {
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
        @DisplayName("성공 — Mock 설정 3줄 필요")
        void signup_성공() {
            // arrange — Mock 설정 3줄
            when(userRepository.find(any(LoginId.class))).thenReturn(Optional.empty());
            when(passwordEncoder.encode("Test1234!@#")).thenReturn("$2a$10$encodedHash");
            //   ↑ [불편함 2] "Test1234!@#"을 넘기면 이 값을 반환해라.
            //   이건 내가 테스트하고 싶은 것이 아니다. 암호화 결과를 지시하는 것.
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            //   ↑ [불편함 2] save가 받은 객체를 그대로 반환해라. 이것도 노이즈.

            // act
            UserModel result = userService.signup(
                validLoginId, validPassword, validName, validBirthDate, validEmail
            );

            // assert
            assertThat(result).isNotNull();
            assertThat(result.getLoginId()).isEqualTo(validLoginId);
            assertThat(result.getPassword().getValue()).isEqualTo("$2a$10$encodedHash");
            //         ↑ 이 값은 내가 Mock에게 "반환하라"고 지시한 값. 실제 암호화 결과가 아님.
        }

        @Test
        @DisplayName("중복 아이디면 예외")
        void signup_중복아이디_예외() {
            // arrange
            UserModel existingUser = createTestUser("$2a$10$existingHash");
            when(userRepository.find(any(LoginId.class))).thenReturn(Optional.of(existingUser));

            // act & assert
            assertThatThrownBy(() ->
                userService.signup(validLoginId, validPassword, validName, validBirthDate, validEmail)
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("이미 존재하는 아이디입니다.");
        }
    }

    @DisplayName("비밀번호 변경")
    @Nested
    class ChangePassword {

        @Test
        @DisplayName("성공 — Mock 설정 5줄 필요 (가장 극적인 불편함)")
        void changePassword_성공() {
            // arrange — Mock 설정 5줄 (!!)
            UserModel existingUser = createTestUser("$2a$10$encodedOldHash");

            when(userRepository.find(any(LoginId.class)))
                .thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("Test1234!@#", "$2a$10$encodedOldHash"))
                .thenReturn(true);
            //   ↑ [불편함 2] "현재 비밀번호가 맞다고 해줘" — Mock에게 연기 지시
            when(passwordEncoder.matches("NewPass123!@", "$2a$10$encodedOldHash"))
                .thenReturn(false);
            //   ↑ [불편함 2] "새 비밀번호는 현재와 다르다고 해줘" — 또 연기 지시
            when(passwordEncoder.encode("NewPass123!@"))
                .thenReturn("$2a$10$encodedNewHash");
            //   ↑ [불편함 2] "새 비밀번호를 암호화하면 이 값을 반환해줘"
            when(userRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

            // act
            userService.changePassword(
                validLoginId,
                Password.of("Test1234!@#"),
                Password.of("NewPass123!@")
            );

            // assert
            verify(userRepository).save(any());
            // ↑ [불편함 3] save가 "호출되었는가?"만 확인 가능.
            //   "어떤 비밀번호로 바뀌었는가?"는 알 수 없다.
            //   Mock이라 실제로 저장되지 않았기 때문.
        }

        @Test
        @DisplayName("현재 비밀번호 불일치면 예외")
        void changePassword_현재비밀번호_불일치() {
            // arrange
            UserModel existingUser = createTestUser("$2a$10$encodedOldHash");

            when(userRepository.find(any(LoginId.class)))
                .thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("Wrong123!@#", "$2a$10$encodedOldHash"))
                .thenReturn(false);

            // act & assert
            assertThatThrownBy(() ->
                userService.changePassword(
                    validLoginId,
                    Password.of("Wrong123!@#"),
                    Password.of("NewPass123!@")
                )
            ).isInstanceOf(CoreException.class)
             .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
        }
    }

    // --- 헬퍼 ---

    private UserModel createTestUser(String encodedPassword) {
        return new UserModel(
            validLoginId,
            Password.fromEncoded(encodedPassword),
            validName,
            validBirthDate,
            validEmail
        );
    }
}
