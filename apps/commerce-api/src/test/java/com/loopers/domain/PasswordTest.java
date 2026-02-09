package com.loopers.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.loopers.support.error.CoreException;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PasswordTest {

    private BirthDate defaultBirthDate;

    @BeforeEach
    void setUp() {
        defaultBirthDate = new BirthDate(LocalDate.of(1990, 1, 15));
    }

    @DisplayName("비밀번호 객체를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("8~16자의 영문 대소문자, 숫자, 특수문자가 모두 포함되면 정상 생성된다.")
        @Test
        void createPassword_whenValidFormat() {
            // 대문자(V), 소문자(alid), 숫자(123), 특수문자(!@#) 모두 포함
            assertDoesNotThrow(() -> Password.of("Valid123!@#", defaultBirthDate));
        }

        @DisplayName("규칙에 어긋나는 형식이면 예외가 발생한다.")
        @Test
        void createPassword_whenInvalidFormat() {
            assertThatThrownBy(() -> Password.of("invalid", defaultBirthDate))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("비밀번호 비즈니스 규칙을 검증할 때, ")
    @Nested
    class Validation {

        @DisplayName("비밀번호에 생년월일(yyyyMMdd)이 포함되어 있으면 예외가 발생한다.")
        @Test
        void validateNotContainBirthday_fail() {
            // act & assert: Password.of 생성 시점에 생년월일 포함 검증이 수행됨
            assertThatThrownBy(() -> Password.of("Pw19900115!", defaultBirthDate))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("수정하려는 비밀번호가 기존 비밀번호와 동일하면 예외가 발생한다.")
        @Test
        void validateNotSameAs_fail() {
            // arrange
            Password currentPassword = Password.of("Current123!", defaultBirthDate);
            Password newPassword = Password.of("Current123!", defaultBirthDate);

            // act & assert
            assertThatThrownBy(() -> newPassword.validateNotSameAs(currentPassword))
                    .isInstanceOf(CoreException.class);
        }
    }
}
