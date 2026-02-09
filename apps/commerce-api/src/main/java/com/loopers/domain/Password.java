package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
public class Password {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+=-])[A-Za-z\\d~!@#$%^&*()_+=-]{8,16}$");

    private String value;

    protected Password() {}

    private Password(String value) {
       this. value = value;
    }

    public static Password of(String rawPassword) {
        validateFormat(rawPassword);
        return new Password(rawPassword);
    }

    public static Password of(String rawPassword, BirthDate birthDate) {
        validateFormat(rawPassword);
        validateNotContainBirthday(rawPassword, birthDate);
        return new Password(rawPassword);
    }

    private Password(String value, boolean skipValidation) {
        this.value = value;
    }

    public static Password fromEncoded(String encodedValue) {
        return new Password(encodedValue, true);
    }

    private static void validateFormat(String value) {
        if (value == null || !PASSWORD_PATTERN.matcher(value).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자 조합이어야 합니다.");
        }
    }

    private static void validateNotContainBirthday(String rawPassword, BirthDate birthDate) {
        String birthDateString = birthDate.toDateString();

        if (rawPassword.contains(birthDateString)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비밀번호 내에 포함될 수 없습니다.");
        }
    }

    public void validateNotContainBirthday(BirthDate birthDate) {
        validateNotContainBirthday(this.value, birthDate);
    }

    public void validateNotSameAs(Password other) {
        if (this.equals(other)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "현재 사용 중인 비밀번호는 사용할 수 없습니다.");
        }
    }

    public String getValue() {
        return value;
    }
}
