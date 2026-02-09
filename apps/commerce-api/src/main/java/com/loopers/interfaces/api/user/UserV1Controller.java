package com.loopers.interfaces.api.user;

import com.loopers.domain.AuthenticationService;
import com.loopers.domain.ChangePasswordCommand;
import com.loopers.domain.SignupCommand;
import com.loopers.domain.UserInfo;
import com.loopers.domain.UserModel;
import com.loopers.domain.UserService;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private static final String HEADER_LOGIN_ID = "X-Loopers-LoginId";
    private static final String HEADER_LOGIN_PW = "X-Loopers-LoginPw";

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    @Override
    public ApiResponse<UserV1Dto.SignupResponse> signup(
        @Valid @RequestBody UserV1Dto.SignupRequest request
    ) {
        SignupCommand command = new SignupCommand(
            request.loginId(), request.password(), request.name(), request.birthDate(), request.email()
        );
        UserInfo userInfo = userService.signup(command);

        return ApiResponse.success(UserV1Dto.SignupResponse.from(userInfo));
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserV1Dto.MyInfoResponse> getMyInfo(
        @RequestHeader(HEADER_LOGIN_ID) String loginId,
        @RequestHeader(HEADER_LOGIN_PW) String password
    ) {
        UserModel authenticatedUser = authenticationService.authenticate(loginId, password);
        UserInfo userInfo = userService.getMyInfo(authenticatedUser.getLoginId());

        return ApiResponse.success(UserV1Dto.MyInfoResponse.from(userInfo));
    }

    @PatchMapping("/password")
    @Override
    public ApiResponse<UserV1Dto.ChangePasswordResponse> changePassword(
        @RequestHeader(HEADER_LOGIN_ID) String loginId,
        @RequestHeader(HEADER_LOGIN_PW) String currentPasswordValue,
        @Valid @RequestBody UserV1Dto.ChangePasswordRequest request
    ) {
        UserModel authenticatedUser = authenticationService.authenticate(loginId, currentPasswordValue);
        ChangePasswordCommand command = new ChangePasswordCommand(
            authenticatedUser.getLoginId(), request.currentPassword(), request.newPassword()
        );
        userService.changePassword(command);

        return ApiResponse.success(UserV1Dto.ChangePasswordResponse.success());
    }
}
