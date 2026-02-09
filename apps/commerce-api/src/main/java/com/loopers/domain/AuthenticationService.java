package com.loopers.domain;

// PasswordEncoder는 이제 같은 domain 패키지에 있으므로 import 불필요
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserModel authenticate(String loginIdValue, String rawPassword) {
        LoginId loginId = new LoginId(loginIdValue);

        UserModel user = userRepository.find(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword().getValue())) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "로그인 ID 또는 비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}
