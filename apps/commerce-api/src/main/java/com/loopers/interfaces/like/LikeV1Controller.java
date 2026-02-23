package com.loopers.interfaces.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.dto.LikeResult;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.auth.Login;
import com.loopers.interfaces.auth.LoginUser;
import com.loopers.interfaces.like.dto.LikeV1Dto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/api/v1/products/{productId}/likes")
    @Override
    public ApiResponse<Object> like(
        @Login LoginUser loginUser,
        @PathVariable Long productId
    ) {
        likeFacade.like(loginUser.id(), productId);
        return ApiResponse.success();
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    @Override
    public ApiResponse<Object> unlike(
        @Login LoginUser loginUser,
        @PathVariable Long productId
    ) {
        likeFacade.unlike(loginUser.id(), productId);
        return ApiResponse.success();
    }

    @GetMapping("/api/v1/users/me/likes")
    @Override
    public ApiResponse<LikeV1Dto.ListResponse> getMyLikes(
        @Login LoginUser loginUser
    ) {
        List<LikeResult> results = likeFacade.getMyLikedProducts(loginUser.id());
        LikeV1Dto.ListResponse listResponse = new LikeV1Dto.ListResponse(
            results.stream()
                .map(LikeV1Dto.ListResponse.ListItem::from)
                .toList()
        );
        return ApiResponse.success(listResponse);
    }
}
