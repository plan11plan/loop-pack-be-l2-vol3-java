package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public PaymentModel createPayment(Long orderId, int amount,
                                      CardType cardType, String maskedCardNo,
                                      String pgProvider) {
        PaymentModel payment = PaymentModel.create(orderId, amount, cardType, maskedCardNo);
        payment.addTransaction(
                PaymentTransactionModel.create(payment, pgProvider, LocalDateTime.now()));
        return paymentRepository.save(payment);
    }

    @Transactional
    public PaymentModel retryPayment(Long orderId, CardType cardType,
                                     String maskedCardNo, String pgProvider) {
        PaymentModel payment = getByOrderIdOrThrow(orderId);
        if (!payment.isRetryable()) {
            throw new CoreException(PaymentErrorCode.PAYMENT_IN_PROGRESS);
        }
        payment.updateCardInfo(cardType, maskedCardNo);
        payment.addTransaction(
                PaymentTransactionModel.create(payment, pgProvider, LocalDateTime.now()));
        return payment;
    }

    @Transactional
    public PaymentModel handleCallback(String paymentKey, PgCallbackStatus callbackStatus,
                                       String pgReason) {
        PaymentModel payment = paymentRepository.findByTransactionPaymentKey(paymentKey)
                .orElseThrow(() -> new CoreException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        PaymentTransactionModel transaction = payment.getTransactions().stream()
                .filter(tx -> paymentKey.equals(tx.getPaymentKey()))
                .findFirst()
                .orElseThrow(() -> new CoreException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.isCompleted() || payment.isApproved() || payment.isFailed()) {
            return payment;
        }

        if (callbackStatus.isSuccess()) {
            transaction.succeed(null);
            payment.approve(LocalDateTime.now());
        } else {
            transaction.fail(callbackStatus.name(), pgReason);
            payment.fail();
        }
        return payment;
    }

    @Transactional
    public void failPayment(Long orderId) {
        getByOrderIdOrThrow(orderId).fail();
    }

    @Transactional(readOnly = true)
    public Optional<PaymentModel> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    private PaymentModel getByOrderIdOrThrow(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(PaymentErrorCode.PAYMENT_NOT_FOUND));
    }
}
