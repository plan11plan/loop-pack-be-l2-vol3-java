package com.loopers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PasswordTest {

    @DisplayName("비밀번호 객체를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("8~16자의 영문 대소문자, 숫자, 특수문자가 모두 포함되면 정상 생성된다.")
        @Test
        void createPassword_whenValidFormat() {
            // 대문자(V), 소문자(alid), 숫자(123), 특수문자(!@#) 모두 포함
            assertDoesNotThrow(() -> new Password("Valid123!@#"));
        }

        @DisplayName("규칙에 어긋나는 형식이면 예외가 발생한다.")
        @Test
        void createPassword_whenInvalidFormat() {
            assertThatThrownBy(() -> new Password("invalid"))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("fromEncoded 정적 팩토리 메서드는")
    @Nested
    class FromEncoded {

        @DisplayName("검증을 건너뛰고 암호화된 비밀번호를 저장한다")
        @Test
        void fromEncoded_should_skip_validation() {
            String encodedPassword = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

            Password password = Password.fromEncoded(encodedPassword);

            assertThat(password.getValue()).isEqualTo(encodedPassword);
        }

        @DisplayName("BCrypt 포맷의 암호화된 비밀번호를 저장할 수 있다")
        @Test
        void fromEncoded_should_accept_bcrypt_format() {
            String bcryptPassword = "$2a$10$AbCdEfGhIjKlMnOpQrStUvWxYz0123456789AbCdEfGhIjKlMnOpQr";

            Password password = Password.fromEncoded(bcryptPassword);

            assertThat(password.getValue()).startsWith("$2a$");
            assertThat(password.getValue()).isEqualTo(bcryptPassword);
        }

        @DisplayName("비밀번호 형식 검증을 거치지 않는다")
        @Test
        void fromEncoded_should_not_validate_password_format() {
            String invalidFormatButEncoded = "$2a$10$short";

            Password password = Password.fromEncoded(invalidFormatButEncoded);

            assertThat(password.getValue()).isEqualTo(invalidFormatButEncoded);
        }
    }

    @DisplayName("비밀번호 비즈니스 규칙을 검증할 때, ")
    @Nested
    class Validation {

        @DisplayName("비밀번호에 생년월일(yyyyMMdd)이 포함되어 있으면 예외가 발생한다.")
        @Test
        void validateNotContainBirthday_fail() {
            // arrange: 정규식을 통과하기 위해 대문자 'P' 추가
            Password password = new Password("Pw19900115!");
            BirthDate birthDate = new BirthDate(LocalDate.of(1990, 1, 15));

            // act & assert
            assertThatThrownBy(() -> password.validateNotContainBirthday(birthDate))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("수정하려는 비밀번호가 기존 비밀번호와 동일하면 예외가 발생한다.")
        @Test
        void validateNotSameAs_fail() {
            // arrange
            Password currentPassword = new Password("Current123!");
            Password newPassword = new Password("Current123!");

            // act & assert
            assertThatThrownBy(() -> newPassword.validateNotSameAs(currentPassword))
                    .isInstanceOf(CoreException.class);
        }
    }
}
