package com.loopers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EncryptedPasswordTest {

    private BirthDate defaultBirthDate;
    private PasswordEncoder noOpEncoder;

    @BeforeEach
    void setUp() {
        defaultBirthDate = new BirthDate(LocalDate.of(1990, 1, 15));
        noOpEncoder = new PasswordEncoder() {
            @Override
            public String encode(String rawPassword) { return rawPassword; }
            @Override
            public boolean matches(String rawPassword, String encodedPassword) { return rawPassword.equals(encodedPassword); }
        };
    }

    @DisplayName("암호화된 비밀번호 객체를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("8~16자의 영문 대소문자, 숫자, 특수문자가 모두 포함되면 정상 생성된다.")
        @Test
        void createPassword_whenValidFormat() {
            assertDoesNotThrow(() -> EncryptedPassword.of("Valid123!@#", noOpEncoder));
        }

        @DisplayName("규칙에 어긋나는 형식이면 예외가 발생한다.")
        @Test
        void createPassword_whenInvalidFormat() {
            assertThatThrownBy(() -> EncryptedPassword.of("invalid", noOpEncoder))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("비밀번호 비즈니스 규칙을 검증할 때, ")
    @Nested
    class Validation {

        @DisplayName("비밀번호에 생년월일(yyyyMMdd)이 포함되어 있으면 예외가 발생한다.")
        @Test
        void validateNotContainBirthday_fail() {
            assertThatThrownBy(() -> EncryptedPassword.of("Pw19900115!", noOpEncoder, defaultBirthDate))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("matches()로 원시 비밀번호와 암호화된 비밀번호를 비교할 수 있다.")
        @Test
        void matches_shouldCompareRawWithEncoded() {
            EncryptedPassword password = EncryptedPassword.of("Valid123!@#", noOpEncoder);

            assertThat(password.matches("Valid123!@#", noOpEncoder)).isTrue();
            assertThat(password.matches("Wrong123!@#", noOpEncoder)).isFalse();
        }
    }
}
