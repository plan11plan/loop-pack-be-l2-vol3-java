package com.loopers.domain.payment;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository {

    PaymentTransactionModel save(PaymentTransactionModel transaction);

    Optional<PaymentTransactionModel> findById(Long id);

    Optional<PaymentTransactionModel> findByPaymentKey(String paymentKey);

    List<PaymentTransactionModel> findByPaymentId(Long paymentId);
}
