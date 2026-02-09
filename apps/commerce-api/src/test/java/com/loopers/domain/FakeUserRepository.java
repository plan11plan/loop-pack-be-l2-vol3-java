package com.loopers.domain;

// ============================================================
// STEP 3: Fake 구현체
//
// UserRepository 인터페이스는 원래부터 domain에 있었으므로
// 이 Fake는 처음부터 infrastructure import 없이 작성 가능했다.
// ============================================================

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
