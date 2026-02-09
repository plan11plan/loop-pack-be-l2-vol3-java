package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
public class EncryptedPassword {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+=-])[A-Za-z\\d~!@#$%^&*()_+=-]{8,16}$");

    private String value;

    protected EncryptedPassword() {}

    private EncryptedPassword(String encryptedValue) {
        this.value = encryptedValue;
    }

    public static EncryptedPassword of(String rawPassword, PasswordEncoder encoder) {
        validateFormat(rawPassword);
        return new EncryptedPassword(encoder.encode(rawPassword));
    }

    public static EncryptedPassword fromEncoded(String encodedValue) {
        return new EncryptedPassword(encodedValue);
    }

    public boolean matches(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.value);
    }

    public String getValue() {
        return value;
    }

    private static void validateFormat(String value) {
        if (value == null || !PASSWORD_PATTERN.matcher(value).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자의 영문 대소문자, 숫자, 특수문자 조합이어야 합니다.");
        }
    }

}