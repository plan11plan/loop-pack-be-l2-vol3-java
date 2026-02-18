package com.loopers.application.user;

import com.loopers.application.user.dto.UserCommand;
import com.loopers.application.user.dto.UserInfo;
import com.loopers.domain.user.AuthenticationService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @Transactional
    public UserInfo signup(UserCommand.Signup command) {
        UserModel userModel = userService.signup(
            command.loginId(), command.rawPassword(), command.name(), command.birthDate(), command.email()
        );
        return UserInfo.from(userModel);
    }

    @Transactional(readOnly = true)
    public UserInfo getMyInfo(String loginId, String password) {
        UserModel authenticatedUser = authenticationService.authenticate(loginId, password);
        UserModel user = userService.getByLoginId(authenticatedUser.getLoginId().getValue());
        return UserInfo.from(user);
    }

    @Transactional
    public void changePassword(String loginId, String currentPassword, UserCommand.ChangePassword command) {
        UserModel authenticatedUser = authenticationService.authenticate(loginId, currentPassword);
        userService.changePassword(
            authenticatedUser.getLoginId().getValue(), command.rawCurrentPassword(), command.rawNewPassword()
        );
    }
}
