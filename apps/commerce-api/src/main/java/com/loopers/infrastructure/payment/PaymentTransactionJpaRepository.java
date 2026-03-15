package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentTransactionModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionModel, Long> {

    Optional<PaymentTransactionModel> findByIdAndDeletedAtIsNull(Long id);

    Optional<PaymentTransactionModel> findByPaymentKeyAndDeletedAtIsNull(String paymentKey);

    List<PaymentTransactionModel> findByPaymentIdAndDeletedAtIsNull(Long paymentId);
}
