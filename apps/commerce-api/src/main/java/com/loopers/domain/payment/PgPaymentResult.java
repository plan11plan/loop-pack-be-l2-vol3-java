package com.loopers.domain.payment;

public record PgPaymentResult(
        boolean requested,
        String transactionKey,
        String status) {
}
