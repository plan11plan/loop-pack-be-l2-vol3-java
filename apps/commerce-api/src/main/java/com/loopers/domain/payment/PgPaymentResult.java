package com.loopers.domain.payment;

public record PgPaymentResult(
        boolean requested,
        String transactionKey,
        String status,
        String errorMessage) {

    public PgPaymentResult(boolean requested, String transactionKey, String status) {
        this(requested, transactionKey, status, null);
    }
}
