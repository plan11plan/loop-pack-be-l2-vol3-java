package com.loopers.interfaces.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentResult;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.auth.Login;
import com.loopers.interfaces.auth.LoginUser;
import com.loopers.interfaces.payment.dto.PaymentRequest;
import com.loopers.interfaces.payment.dto.PaymentResponse;
import com.loopers.interfaces.payment.dto.PgCallbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller {

    private final PaymentFacade paymentFacade;

    @PostMapping
    public ApiResponse<PaymentResponse.PaymentSummary> requestPayment(
            @Login LoginUser loginUser,
            @Valid @RequestBody PaymentRequest.Create request) {
        PaymentResult result = paymentFacade.requestPayment(
                loginUser.id(), request.toCriteria());
        return ApiResponse.success(PaymentResponse.PaymentSummary.from(result));
    }

    @PostMapping("/callback")
    public ApiResponse<Object> handleCallback(
            @RequestBody PgCallbackRequest request) {
        String failureCode = parseFailureCode(request.reason());
        paymentFacade.handleCallback(
                request.transactionKey(), request.status(),
                failureCode, request.reason());
        return ApiResponse.success();
    }

    private String parseFailureCode(String reason) {
        if (reason == null) {
            return null;
        }
        if (reason.contains("한도초과")) {
            return "LIMIT_EXCEEDED";
        }
        if (reason.contains("잘못된 카드")) {
            return "INVALID_CARD";
        }
        return "PG_ERROR";
    }
}
