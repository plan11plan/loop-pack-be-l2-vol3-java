package com.loopers.domain;

public record UserInfo(
    Long id,
    String loginId,
    String name,
    String birthDate,
    String email
) {
    public static UserInfo from(UserModel model) {
        return new UserInfo(
            model.getId(),
            model.getLoginId().getValue(),
            model.getName().getValue(),
            model.getBirthDate().toDateString(),
            model.getEmail().getMail()
        );
    }
}
