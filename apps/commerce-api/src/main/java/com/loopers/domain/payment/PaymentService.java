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
    private final PaymentTransactionRepository transactionRepository;

    @Transactional
    public PaymentModel createPayment(Long orderId, int amount,
                                      CardType cardType, String maskedCardNo,
                                      String pgProvider) {
        PaymentModel payment = paymentRepository.save(
                PaymentModel.create(orderId, amount, cardType, maskedCardNo));
        PaymentTransactionModel transaction = PaymentTransactionModel.create(
                payment, pgProvider, LocalDateTime.now());
        payment.addTransaction(transaction);
        transactionRepository.save(transaction);
        return payment;
    }

    @Transactional
    public PaymentModel retryPayment(Long orderId, CardType cardType,
                                     String maskedCardNo, String pgProvider) {
        PaymentModel payment = getByOrderIdOrThrow(orderId);
        if (!payment.isRetryable()) {
            throw new CoreException(PaymentErrorCode.PAYMENT_IN_PROGRESS);
        }
        payment.updateCardInfo(cardType, maskedCardNo);
        PaymentTransactionModel transaction = PaymentTransactionModel.create(
                payment, pgProvider, LocalDateTime.now());
        payment.addTransaction(transaction);
        transactionRepository.save(transaction);
        return payment;
    }

    @Transactional
    public PaymentModel handleCallback(String paymentKey, String pgStatus,
                                       String failureCode, String failureMessage) {
        PaymentTransactionModel transaction = transactionRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new CoreException(PaymentErrorCode.TRANSACTION_NOT_FOUND));

        if (transaction.isCompleted()) {
            return transaction.getPayment();
        }

        PaymentModel payment = transaction.getPayment();
        if (payment.isApproved() || payment.isFailed()) {
            return payment;
        }

        if ("SUCCESS".equals(pgStatus)) {
            transaction.succeed(null);
            payment.approve(LocalDateTime.now());
        } else {
            transaction.fail(failureCode, failureMessage);
        }
        return payment;
    }

    @Transactional
    public void failPayment(Long orderId) {
        PaymentModel payment = getByOrderIdOrThrow(orderId);
        payment.fail();
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
