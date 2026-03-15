package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentTransactionModel;
import com.loopers.domain.payment.PaymentTransactionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentTransactionRepositoryImpl implements PaymentTransactionRepository {

    private final PaymentTransactionJpaRepository jpaRepository;

    @Override
    public PaymentTransactionModel save(PaymentTransactionModel transaction) {
        return jpaRepository.save(transaction);
    }

    @Override
    public Optional<PaymentTransactionModel> findById(Long id) {
        return jpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<PaymentTransactionModel> findByPaymentKey(String paymentKey) {
        return jpaRepository.findByPaymentKeyAndDeletedAtIsNull(paymentKey);
    }

    @Override
    public List<PaymentTransactionModel> findByPaymentId(Long paymentId) {
        return jpaRepository.findByPaymentIdAndDeletedAtIsNull(paymentId);
    }
}
