package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class Password {
    private final String password;

    public Password(String password) {
        this.password = password;
    }

    public void validateNotContainBirthday(BirthDate birthDate) {
        String targetDate = birthDate.toDateString();

        if (this.password.contains(targetDate)) {
            throw new CoreException(ErrorType.BAD_REQUEST,"생년월일은 비밀번호 내에 포함될 수 없습니다");
        }
    }
}
