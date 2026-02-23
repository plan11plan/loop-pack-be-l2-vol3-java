package com.loopers.domain.like;

import com.loopers.support.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductLikeErrorCode implements ErrorCode {
    DUPLICATE_LIKE(HttpStatus.CONFLICT, "PRODUCT_LIKE_001", "이미 좋아요가 됐습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
