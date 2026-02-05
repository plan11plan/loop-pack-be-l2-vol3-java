package com.loopers.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.infrastructure.PasswordEncoder;
import com.loopers.infrastructure.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private LoginId validLoginId;
    private Password validPassword;
    private String rawPassword;
    private Name validName;
    private BirthDate validBirthDate;
    private Email validEmail;

    @BeforeEach
    void setUp() {
        validLoginId = new LoginId("testuser123");
        rawPassword = "Test1234!@#";
        validPassword = new Password(rawPassword);
        validName = new Name("홍길동");
        validBirthDate = new BirthDate(LocalDate.of(1990, 1, 15));
        validEmail = new Email("test@example.com");
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("유저가 회원가입할 때")
    @Nested
    class SingUp{
            @DisplayName("로그인 ID, 비밀번호, 이름, 생년월일, 이메일을 주면, 회원가입을 한다.")
            @Test
            void signup_whenAllInfoProvided() {
                // act
                UserModel result = userService.signup(validLoginId,validPassword,validName,validBirthDate,validEmail);

                // assert
                assertAll(
                        () -> assertThat(result).isNotNull(),
                        () -> assertThat(result.getLoginId()).isEqualTo(validLoginId),
                        () -> assertThat(result.getName()).isEqualTo(validName),
                        () -> assertThat(result.getBirthDate()).isEqualTo(validBirthDate),
                        () -> assertThat(result.getEmail()).isEqualTo(validEmail)
                );
            }

            @DisplayName("비밀번호를 암호화하여 저장한다")
            @Test
            void signup_should_encrypt_password() {
                // act
                UserModel result = userService.signup(validLoginId,validPassword,validName,validBirthDate,validEmail);

                // assert
                String savedPassword = result.getPassword().getValue();
                assertAll(
                        () -> assertThat(savedPassword).isNotEqualTo(rawPassword), // 평문과 다름
                        () -> assertThat(savedPassword).startsWith("$2a$"), // BCrypt 포맷
                        () -> assertThat(passwordEncoder.matches(rawPassword, savedPassword)).isTrue() // 평문과 매칭됨
                );
            }

            @DisplayName("DB에 저장된 비밀번호가 암호화되어 있다")
            @Test
            void signup_should_save_encrypted_password_to_database() {
                // act
                UserModel result = userService.signup(validLoginId,validPassword,validName,validBirthDate,validEmail);

                // assert
                UserModel savedUser = userJpaRepository.findById(result.getId()).orElseThrow();
                String savedPassword = savedUser.getPassword().getValue();
                assertAll(
                        () -> assertThat(savedPassword).isNotEqualTo(rawPassword), // 평문과 다름
                        () -> assertThat(savedPassword).startsWith("$2a$"), // BCrypt 포맷
                        () -> assertThat(passwordEncoder.matches(rawPassword, savedPassword)).isTrue() // 평문과 매칭됨
                );
            }

            @DisplayName("비밀번호에 생년월일이 포함되면 예외가 발생한다")
            @Test
            void signup_should_throw_exception_when_password_contains_birthdate() {
                // arrange
                BirthDate birthDate = new BirthDate(LocalDate.of(1990, 1, 15));
                Password passwordWithBirthDate = new Password("Pw19900115!");

                // expect
                assertThatThrownBy(() -> userService.signup(validLoginId, passwordWithBirthDate, validName, birthDate, validEmail))
                        .isInstanceOf(CoreException.class)
                        .hasMessageContaining("생년월일은 비밀번호 내에 포함될 수 없습니다.");
            }
    }
}
