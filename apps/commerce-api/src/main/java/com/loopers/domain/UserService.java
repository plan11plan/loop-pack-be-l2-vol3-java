package com.loopers.domain;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
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
    public UserInfo signup(SignupCommand command) {
        LoginId loginId = new LoginId(command.loginId());
        Name name = new Name(command.name());
        BirthDate birthDate = new BirthDate(LocalDate.parse(command.birthDate(), BIRTH_DATE_FORMATTER));
        Email email = new Email(command.email());

        if(userRepository.find(loginId).isPresent()) {
            throw new CoreException(ErrorType.BAD_REQUEST,"이미 존재하는 아이디입니다.");
        }

        EncryptedPassword password = EncryptedPassword.of(command.rawPassword(), passwordEncoder, birthDate);
        UserModel userModel = new UserModel(loginId, password, name, birthDate, email);

        return UserInfo.from(userRepository.save(userModel));
    }

    public UserInfo getMyInfo(LoginId loginId) {
        UserModel user = userRepository.find(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return UserInfo.from(user);
    }

    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        UserModel user = userRepository.find(command.loginId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        user.changePassword(command.rawCurrentPassword(), command.rawNewPassword(), passwordEncoder);
        userRepository.save(user);
    }
}
