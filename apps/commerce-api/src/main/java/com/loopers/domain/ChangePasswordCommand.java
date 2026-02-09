package com.loopers.domain;

public record ChangePasswordCommand(
    LoginId loginId,
    String rawCurrentPassword,
    String rawNewPassword
) {
}
