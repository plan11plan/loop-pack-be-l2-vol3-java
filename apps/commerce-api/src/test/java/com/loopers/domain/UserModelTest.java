package com.loopers.domain;

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

    private LoginId validLoginId;
    private Password validPassword;
    private Name validName;
    private BirthDate validBirthDate;
    private Email validEmail;

    @BeforeEach
    void setUp() {
        validLoginId = new LoginId("testuser123");
        validPassword = new Password("Test1234!@#");
        validName = new Name("홍길동");
        validBirthDate = new BirthDate(LocalDate.of(1990, 1, 15));
        validEmail = new Email("test@example.com");
    }

    @DisplayName("유저 모델을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("모든 필드가 주어지면, 정상적으로 생성된다.")
        @Test
        void createUserModel_whenAllDataProvided() {
            // act
            UserModel user = new UserModel(validLoginId, validPassword, validName, validBirthDate, validEmail);

            // assert
            assertAll(
                    () -> assertThat(user.getLoginId()).isEqualTo(validLoginId),
                    () -> assertThat(user.getPassword()).isEqualTo(validPassword),
                    () -> assertThat(user.getName()).isEqualTo(validName),
                    () -> assertThat(user.getBirthDate()).isEqualTo(validBirthDate),
                    () -> assertThat(user.getEmail()).isEqualTo(validEmail)
            );
        }

        @DisplayName("로그인 ID가 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenLoginIdIsNull() {
            assertThatThrownBy(() -> new UserModel(null, validPassword, validName, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("비밀번호가 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenPasswordIsNull() {
            assertThatThrownBy(() -> new UserModel(validLoginId, null, validName, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("이름이 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenNameIsNull() {
            assertThatThrownBy(() -> new UserModel(validLoginId, validPassword, null, validBirthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("생년월일이 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenBirthDateIsNull() {
            assertThatThrownBy(() -> new UserModel(validLoginId, validPassword, validName, null, validEmail))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("이메일이 누락되면 예외가 발생한다.")
        @Test
        void createUserModel_whenEmailIsNull() {
            assertThatThrownBy(() -> new UserModel(validLoginId, validPassword, validName, validBirthDate, null))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("비밀번호에 생년월일이 포함되면 예외가 발생한다.")
        @Test
        void createUserModel_whenPasswordContainsBirthDate() {
            // arrange
            BirthDate birthDate = new BirthDate(LocalDate.of(1990, 1, 15));
            Password passwordWithBirthDate = new Password("Pw19900115!");

            // expect
            assertThatThrownBy(() -> new UserModel(validLoginId, passwordWithBirthDate, validName, birthDate, validEmail))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("비밀번호를 변경할 때, ")
    @Nested
    class ChangePassword {

        private UserModel user;

        @BeforeEach
        void setUp() {
            user = new UserModel(validLoginId, validPassword, validName, validBirthDate, validEmail);
        }

        @DisplayName("올바른 현재 비밀번호와 새 비밀번호를 주면, 비밀번호가 변경된다.")
        @Test
        void changePassword_whenValidPasswords() {
            // arrange
            Password newPassword = new Password("NewPass123!@");

            // act
            user.changePassword(validPassword, newPassword);

            // assert
            assertThat(user.getPassword()).isEqualTo(newPassword);
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면 예외가 발생한다.")
        @Test
        void changePassword_whenCurrentPasswordNotMatch() {
            // arrange
            Password wrongPassword = new Password("Wrong123!@#");
            Password newPassword = new Password("NewPass123!@");

            // act & assert
            assertThatThrownBy(() -> user.changePassword(wrongPassword, newPassword))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("현재 비밀번호가 일치하지 않습니다.");
        }

        @DisplayName("새 비밀번호가 현재 비밀번호와 동일하면 예외가 발생한다.")
        @Test
        void changePassword_whenNewPasswordSameAsCurrent() {
            // arrange
            Password samePassword = new Password("Test1234!@#");

            // act & assert
            assertThatThrownBy(() -> user.changePassword(validPassword, samePassword))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("현재 사용 중인 비밀번호는 사용할 수 없습니다.");
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면 예외가 발생한다.")
        @Test
        void changePassword_whenNewPasswordContainsBirthDate() {
            // arrange
            Password newPasswordWithBirthDate = new Password("Pw19900115!");

            // act & assert
            assertThatThrownBy(() -> user.changePassword(validPassword, newPasswordWithBirthDate))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }
}
