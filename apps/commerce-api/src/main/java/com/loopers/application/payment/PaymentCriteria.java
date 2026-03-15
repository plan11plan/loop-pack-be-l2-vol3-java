package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;

public class PaymentCriteria {

    public record Create(Long orderId, CardType cardType, String cardNo) {
    }
}
