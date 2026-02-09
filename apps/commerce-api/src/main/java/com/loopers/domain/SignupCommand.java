package com.loopers.domain;

public record SignupCommand(
    String loginId,
    String rawPassword,
    String name,
    String birthDate,
    String email
) {
}
