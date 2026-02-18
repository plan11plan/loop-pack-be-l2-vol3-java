package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserModelTest {

    private String validLoginId;
    private String validRawPassword;
    private String validName;
    private LocalDate validBirthDate;
    private String validEmail;
    private FakePasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new FakePasswordEncoder();
        validLoginId = "testuser123";
        validRawPassword = "Test1234!@#";
        validName = "홍길동";
        validBirthDate = LocalDate.of(1990, 1, 15);
        validEmail = "test@example.com";
    }

    @DisplayName("유저 모델을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("모든 필드가 주어지면, 정상적으로 생성된다.")
        @Test
        void createUserModel_whenAllDataProvided() {
            // act
            UserModel user = UserModel.create(validLoginId, validRawPassword, encoder, validName, validBirthDate, validEmail);

            // assert
            assertAll(
                    () -> assertThat(user.getLoginId().getValue()).isEqualTo(validLoginId),
                    () -> assertThat(user.getPassword().getValue()).isEqualTo("ENCODED_Test1234!@#"),
                    () -> assertThat(user.getName().getValue()).isEqualTo(validName),
                    () -> assertThat(user.getBirthDate().getDate()).isEqualTo(validBirthDate),
                    () -> assertThat(user.getEmail().getMail()).isEqualTo(validEmail)
            );
        }

        @DisplayName("로그인 ID가 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenLoginIdIsNull() {
            assertThatThrownBy(() -> UserModel.create(null, validRawPassword, encoder, validName, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("비밀번호가 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenPasswordIsNull() {
            assertThatThrownBy(() -> UserModel.create(validLoginId, null, encoder, validName, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("이름이 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenNameIsNull() {
            assertThatThrownBy(() -> UserModel.create(validLoginId, validRawPassword, encoder, null, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("생년월일이 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenBirthDateIsNull() {
            assertThatThrownBy(() -> UserModel.create(validLoginId, validRawPassword, encoder, validName, null, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("이메일이 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenEmailIsNull() {
            assertThatThrownBy(() -> UserModel.create(validLoginId, validRawPassword, encoder, validName, validBirthDate, null))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("비밀번호에 생년월일이 포함되면 예외가 발생한다.")
        @Test
        void createUserModel_whenPasswordContainsBirthDate() {
            assertThatThrownBy(() -> UserModel.create(validLoginId, "Pw19900115!", encoder, validName, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }

    @DisplayName("비밀번호를 변경할 때, ")
    @Nested
    class ChangePassword {

        @DisplayName("올바른 현재 비밀번호와 유효한 새 비밀번호가 주어지면 비밀번호가 변경된다.")
        @Test
        void changePassword_success() {
            // arrange
            UserModel user = UserModel.create(validLoginId, validRawPassword, encoder, validName, validBirthDate, validEmail);

            // act
            user.changePassword("Test1234!@#", "NewPass123!@", encoder);

            // assert
            assertThat(user.getPassword().getValue()).isEqualTo("ENCODED_NewPass123!@");
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다.")
        @Test
        void changePassword_whenCurrentPasswordNotMatch() {
            // arrange
            UserModel user = UserModel.create(validLoginId, validRawPassword, encoder, validName, validBirthDate, validEmail);

            // act & assert
            assertThatThrownBy(() -> user.changePassword("Wrong123!@#", "NewPass123!@", encoder))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
        }

        @DisplayName("새 비밀번호가 현재 비밀번호와 동일하면 예외가 발생한다.")
        @Test
        void changePassword_whenNewPasswordSameAsCurrent() {
            // arrange
            UserModel user = UserModel.create(validLoginId, validRawPassword, encoder, validName, validBirthDate, validEmail);

            // act & assert
            assertThatThrownBy(() -> user.changePassword("Test1234!@#", "Test1234!@#", encoder))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("현재 사용 중인 비밀번호는 사용할 수 없습니다.");
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면 예외가 발생한다.")
        @Test
        void changePassword_whenNewPasswordContainsBirthDate() {
            // arrange
            UserModel user = UserModel.create(validLoginId, validRawPassword, encoder, validName, validBirthDate, validEmail);

            // act & assert
            assertThatThrownBy(() -> user.changePassword("Test1234!@#", "Pw19900115!", encoder))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }
}
