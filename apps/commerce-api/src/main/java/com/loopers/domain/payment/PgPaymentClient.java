package com.loopers.domain.payment;

public interface PgPaymentClient {

    PgPaymentResult requestPayment(PgPaymentRequest request);

    PgTransactionDetail getPaymentStatus(String transactionKey, String userId);

    PgOrderTransactions getPaymentsByOrder(String orderId, String userId);
}
