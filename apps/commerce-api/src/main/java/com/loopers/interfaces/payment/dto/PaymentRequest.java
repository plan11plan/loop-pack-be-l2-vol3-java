package com.loopers.interfaces.payment.dto;

import com.loopers.application.payment.PaymentCriteria;
import com.loopers.domain.payment.CardType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentRequest {

    public record Create(
            @NotNull(message = "주문 ID는 필수 입력값입니다.")
            Long orderId,
            @NotNull(message = "카드 종류는 필수 입력값입니다.")
            CardType cardType,
            @NotBlank(message = "카드 번호는 필수 입력값입니다.")
            String cardNo
    ) {
        public PaymentCriteria.Create toCriteria() {
            return new PaymentCriteria.Create(orderId, cardType, cardNo);
        }
    }
}
