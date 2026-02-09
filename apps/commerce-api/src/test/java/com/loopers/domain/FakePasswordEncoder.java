package com.loopers.domain;

// ============================================================
// STEP 3: Fake 구현체
//
// Mock과 달리, 단순한 로직을 직접 구현한 테스트용 객체.
// when-then 지시 없이 스스로 동작한다.
//
// 핵심: PasswordEncoder 인터페이스가 domain에 있기 때문에
//       이 Fake도 infrastructure import 없이 작성 가능하다.
// ============================================================

public class FakePasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(String rawPassword) {
        return "ENCODED_" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encodedPassword.equals("ENCODED_" + rawPassword);
    }
}
