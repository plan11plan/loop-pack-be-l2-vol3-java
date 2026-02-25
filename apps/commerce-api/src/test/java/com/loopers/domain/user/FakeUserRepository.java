package com.loopers.domain.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeUserRepository implements UserRepository {

    private final Map<String, UserModel> store = new HashMap<>();

    @Override
    public UserModel save(UserModel userModel) {
        store.put(userModel.getLoginId(), userModel);
        return userModel;
    }

    @Override
    public Optional<UserModel> findByLoginId(String loginId) {
        return Optional.ofNullable(store.get(loginId));
    }
}
