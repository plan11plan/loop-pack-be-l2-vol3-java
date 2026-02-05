package com.loopers.domain;

import com.loopers.infrastructure.PasswordEncoder;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserModel signup(LoginId loginId, Password password, Name name, BirthDate birthDate, Email email) {

        if(userRepository.find(loginId).isPresent()) {
            throw new CoreException(ErrorType.BAD_REQUEST,"이미 존재하는 아이디입니다.");
        }

        password.validateNotContainBirthday(birthDate);

        String encodedPasswordValue = passwordEncoder.encode(password.getValue());
        Password encryptedPassword = Password.fromEncoded(encodedPasswordValue);

        UserModel userModel = new UserModel(loginId,encryptedPassword,name,birthDate,email);

        return userRepository.save(userModel);
    }
}
