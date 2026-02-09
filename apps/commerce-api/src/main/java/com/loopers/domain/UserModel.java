package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserModel extends BaseEntity {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "login_id"))
    private LoginId loginId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password"))
    private EncryptedPassword password;

    @Embedded
    @AttributeOverride(name = "name", column = @Column(name = "name"))
    private Name name;

    @Embedded
    @AttributeOverride(name = "birthDate", column = @Column(name = "birth_date"))
    private BirthDate birthDate;

    @Embedded
    @AttributeOverride(name = "mail", column = @Column(name = "email"))
    private Email email;

    public UserModel(LoginId loginId, String rawPassword, PasswordEncoder encoder, Name name, BirthDate birthDate, Email email) {
        validateNotNull(loginId, "로그인 ID");
        validateNotNull(name, "이름");
        validateNotNull(birthDate, "생년월일");
        validateNotNull(email, "이메일");
        validateBirthDateNotInPassword(rawPassword, birthDate);

        this.loginId = loginId;
        this.password = EncryptedPassword.of(rawPassword, encoder);
        this.name = name;
        this.birthDate = birthDate;
        this.email = email;
    }

    private void validateNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, fieldName + "은(는) 필수 입력값입니다.");
        }
    }

    private void validateBirthDateNotInPassword(String rawPassword, BirthDate birthDate) {
        if (rawPassword != null && rawPassword.contains(birthDate.toDateString())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }

    public void changePassword(String rawCurrentPassword, String rawNewPassword, PasswordEncoder encoder) {
        if (!this.password.matches(rawCurrentPassword, encoder)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
        }
        if (this.password.matches(rawNewPassword, encoder)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "현재 사용 중인 비밀번호는 사용할 수 없습니다.");
        }
        validateBirthDateNotInPassword(rawNewPassword, this.birthDate);
        this.password = EncryptedPassword.of(rawNewPassword, encoder);
    }
}
