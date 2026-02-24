package com.loopers.domain.order;

import com.loopers.support.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "주문을 찾을 수 없습니다."),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "ORDER_002", "주문 항목이 비어있습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "ORDER_003", "본인의 주문만 조회할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
