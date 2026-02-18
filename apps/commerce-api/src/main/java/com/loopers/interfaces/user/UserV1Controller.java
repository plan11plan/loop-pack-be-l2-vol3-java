package com.loopers.interfaces.user;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.dto.UserInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.user.dto.UserV1Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserV1Controller implements UserV1ApiSpec {

    private static final String HEADER_LOGIN_ID = "X-Loopers-LoginId";
    private static final String HEADER_LOGIN_PW = "X-Loopers-LoginPw";

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
    public ApiResponse<UserV1Dto.MyInfoResponse> getMyInfo(
        @RequestHeader(HEADER_LOGIN_ID) String loginId,
        @RequestHeader(HEADER_LOGIN_PW) String password
    ) {
        UserInfo userInfo = userFacade.getMyInfo(loginId, password);

        return ApiResponse.success(UserV1Dto.MyInfoResponse.from(userInfo));
    }

    @PatchMapping("/password")
    @Override
    public ApiResponse<UserV1Dto.ChangePasswordResponse> changePassword(
        @RequestHeader(HEADER_LOGIN_ID) String loginId,
        @RequestHeader(HEADER_LOGIN_PW) String currentPasswordValue,
        @Valid @RequestBody UserV1Dto.ChangePasswordRequest request
    ) {
        userFacade.changePassword(loginId, currentPasswordValue, request.toCommand());

        return ApiResponse.success(UserV1Dto.ChangePasswordResponse.success());
    }
}
