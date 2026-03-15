package com.loopers.domain.payment;

import com.loopers.support.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001", "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_002", "결제할 수 없는 주문 상태입니다."),
    ORDER_EXPIRED(HttpStatus.BAD_REQUEST, "PAYMENT_003", "주문이 만료되었습니다."),
    PAYMENT_IN_PROGRESS(HttpStatus.CONFLICT, "PAYMENT_004", "이전 결제가 처리 중입니다."),
    PG_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_005", "결제 서비스가 일시적으로 지연되고 있습니다."),
    PG_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "PAYMENT_006", "결제 요청에 실패했습니다."),
    NOT_PENDING_PAYMENT(HttpStatus.BAD_REQUEST, "PAYMENT_007", "결제 대기 중인 주문만 취소할 수 있습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_008", "결제 정보를 찾을 수 없습니다."),
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_009", "결제 트랜잭션을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
