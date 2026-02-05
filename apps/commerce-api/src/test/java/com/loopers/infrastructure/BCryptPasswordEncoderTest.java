package com.loopers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BCryptPasswordEncoder 단위 테스트")
class BCryptPasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoderImpl();
    }

    @Test
    @DisplayName("동일한 평문 비밀번호를 암호화하면 매번 다른 값이 생성된다")
    void encode_should_return_different_values_for_same_input() {
        String rawPassword = "Password1!";

        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        assertThat(encoded1).isNotEqualTo(encoded2);
        assertThat(encoded1).startsWith("$2a$");
        assertThat(encoded2).startsWith("$2a$");
    }

    @Test
    @DisplayName("암호화된 비밀번호와 평문 비밀번호가 일치하면 true를 반환한다")
    void matches_should_return_true_for_correct_password() {
        String rawPassword = "Password1!";
        String encoded = passwordEncoder.encode(rawPassword);

        boolean result = passwordEncoder.matches(rawPassword, encoded);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("암호화된 비밀번호와 잘못된 평문 비밀번호는 false를 반환한다")
    void matches_should_return_false_for_incorrect_password() {
        String rawPassword = "Password1!";
        String wrongPassword = "WrongPass2@";
        String encoded = passwordEncoder.encode(rawPassword);

        boolean result = passwordEncoder.matches(wrongPassword, encoded);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 문자열을 암호화할 수 있다")
    void encode_should_handle_empty_string() {
        String emptyPassword = "";

        String encoded = passwordEncoder.encode(emptyPassword);

        assertThat(encoded).isNotEmpty();
        assertThat(passwordEncoder.matches(emptyPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("특수문자가 포함된 비밀번호를 암호화할 수 있다")
    void encode_should_handle_special_characters() {
        String specialPassword = "P@ssw0rd!#$%^&*()_+-=";

        String encoded = passwordEncoder.encode(specialPassword);

        assertThat(passwordEncoder.matches(specialPassword, encoded)).isTrue();
    }

    @Test
    @DisplayName("긴 비밀번호를 암호화할 수 있다")
    void encode_should_handle_long_password() {
        String longPassword = "ThisIsAVeryLongPassword123!@#ThisIsAVeryLongPassword123!@#";

        String encoded = passwordEncoder.encode(longPassword);

        assertThat(passwordEncoder.matches(longPassword, encoded)).isTrue();
    }
}
