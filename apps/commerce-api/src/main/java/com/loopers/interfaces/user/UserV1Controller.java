package com.loopers.interfaces.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.dto.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.auth.Auth;
import com.loopers.interfaces.auth.AuthUser;
import com.loopers.interfaces.user.dto.UserV1Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private final UserFacade userFacade;

    @PostMapping("/signup")
    @Override
    public ApiResponse<UserV1Dto.SignupResponse> signup(
        @Valid @RequestBody UserV1Dto.SignupRequest request
    ) {
        UserInfo userInfo = userFacade.signup(request.toCommand());

        return ApiResponse.success(UserV1Dto.SignupResponse.from(userInfo));
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<UserV1Dto.MyInfoResponse> getMyInfo(@Auth AuthUser authUser) {
        UserInfo userInfo = userFacade.getMyInfo(authUser.loginId());

        return ApiResponse.success(UserV1Dto.MyInfoResponse.from(userInfo));
    }

    @PatchMapping("/password")
    @Override
    public ApiResponse<UserV1Dto.ChangePasswordResponse> changePassword(
        @Auth AuthUser authUser,
        @Valid @RequestBody UserV1Dto.ChangePasswordRequest request
    ) {
        userFacade.changePassword(authUser.loginId(), request.toCommand());

        return ApiResponse.success(UserV1Dto.ChangePasswordResponse.success());
    }
}
