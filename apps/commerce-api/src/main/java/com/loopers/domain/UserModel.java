package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserModel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private LoginId loginId;

    @Embedded
    private Password password;

    @Embedded
    private Name name;

    @Embedded
    private BirthDate birthDate;

    @Embedded
    private Email email;

    public UserModel(LoginId loginId, Password password, Name name, BirthDate birthDate, Email email) {
        validate(loginId, password, name, birthDate, email);
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.birthDate = birthDate;
        this.email = email;
    }

    private void validate(LoginId loginId, Password password, Name name, BirthDate birthDate, Email email) {
        validateNotNull(loginId, "로그인 ID");
        validateNotNull(password, "비밀번호");
        validateNotNull(name, "이름");
        validateNotNull(birthDate, "생년월일");
        validateNotNull(email, "이메일");

        password.validateNotContainBirthday(birthDate);
    }
    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new CoreException(ErrorType.BAD_REQUEST,fieldName + "은(는) 필수 입력값입니다.");
        }
    }
}
