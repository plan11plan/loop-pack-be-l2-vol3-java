package com.loopers.domain.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeUserRepository implements UserRepository {

    private final Map<String, UserModel> store = new HashMap<>();

    @Override
    public UserModel save(UserModel userModel) {
        store.put(userModel.getLoginId().getValue(), userModel);
        return userModel;
    }

    @Override
    public Optional<UserModel> find(LoginId loginId) {
        return Optional.ofNullable(store.get(loginId.getValue()));
    }
}
