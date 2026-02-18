package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
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

    // === 생성 === //

    public static UserModel create(String loginId, String rawPassword, PasswordEncoder encoder,
                                   String name, LocalDate birthDate, String email) {
        UserModel model = new UserModel();
        model.loginId = new LoginId(loginId);
        model.name = new Name(name);
        model.birthDate = new BirthDate(birthDate);
        model.email = new Email(email);
        model.validateBirthDateNotInPassword(rawPassword, model.birthDate);
        model.password = EncryptedPassword.of(rawPassword, encoder);
        return model;
    }

    // === 도메인 로직 === //

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

    // === 검증 === //

    private void validateBirthDateNotInPassword(String rawPassword, BirthDate birthDate) {
        if (rawPassword != null && rawPassword.contains(birthDate.toDateString())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }
}
