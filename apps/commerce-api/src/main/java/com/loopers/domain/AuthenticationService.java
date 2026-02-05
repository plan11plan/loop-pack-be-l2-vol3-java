package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;

    public UserModel authenticate(String loginIdValue, String rawPassword) {
        LoginId loginId = new LoginId(loginIdValue);
        Password password = new Password(rawPassword);

        UserModel user = userRepository.find(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 일치하지 않습니다."));

        if (!user.getPassword().equals(password)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
