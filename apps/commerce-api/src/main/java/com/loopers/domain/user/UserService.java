package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserModel signup(String loginId, String rawPassword, String name, String birthDate, String email) {
        if (userRepository.find(new LoginId(loginId)).isPresent()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 존재하는 아이디입니다.");
        }

        UserModel userModel = UserModel.create(
            loginId, rawPassword, passwordEncoder, name,
            LocalDate.parse(birthDate, BIRTH_DATE_FORMATTER), email
        );

        return userRepository.save(userModel);
    }

    @Transactional(readOnly = true)
    public UserModel getByLoginId(String loginId) {
        return userRepository.find(new LoginId(loginId))
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public void changePassword(String loginId, String rawCurrentPassword, String rawNewPassword) {
        UserModel user = userRepository.find(new LoginId(loginId))
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.changePassword(rawCurrentPassword, rawNewPassword, passwordEncoder);
        userRepository.save(user);
    }
}
