package com.loopers.application.user;

import com.loopers.application.user.dto.UserCommand;
import com.loopers.application.user.dto.UserInfo;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;

    @Transactional
    public UserInfo signup(UserCommand.Signup command) {
        UserModel userModel = userService.signup(
            command.loginId(), command.rawPassword(), command.name(), command.birthDate(), command.email()
        );
        return UserInfo.from(userModel);
    }

    @Transactional(readOnly = true)
    public UserInfo getMyInfo(String loginId) {
        UserModel user = userService.getByLoginId(loginId);
        return UserInfo.from(user);
    }

    @Transactional
    public void changePassword(String loginId, UserCommand.ChangePassword command) {
        userService.changePassword(loginId, command.rawCurrentPassword(), command.rawNewPassword());
    }
}
