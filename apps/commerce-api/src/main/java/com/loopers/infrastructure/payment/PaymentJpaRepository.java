package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentModel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentJpaRepository extends JpaRepository<PaymentModel, Long> {

    Optional<PaymentModel> findByOrderIdAndDeletedAtIsNull(Long orderId);

    @Query("SELECT p FROM PaymentModel p JOIN p.transactions t "
            + "WHERE t.paymentKey = :paymentKey AND p.deletedAt IS NULL")
    Optional<PaymentModel> findByTransactionPaymentKeyAndDeletedAtIsNull(String paymentKey);
}
