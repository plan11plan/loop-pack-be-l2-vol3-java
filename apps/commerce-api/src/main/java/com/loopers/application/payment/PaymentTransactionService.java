package com.loopers.application.payment;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentErrorCode;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.support.error.CoreException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private static final String PG_PROVIDER = "PG_SIMULATOR";

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Transactional
    public PaymentModel createOrRetryPayment(PaymentCriteria.Create criteria) {
        OrderModel order = orderService.getByIdWithLock(criteria.orderId());
        if (!order.isPendingPayment()) {
            throw new CoreException(PaymentErrorCode.INVALID_ORDER_STATUS);
        }

        String maskedCardNo = maskCardNo(criteria.cardNo());
        Optional<PaymentModel> existing = paymentService.findByOrderId(criteria.orderId());

        if (existing.isEmpty()) {
            return paymentService.createPayment(
                    criteria.orderId(), order.getTotalPrice(),
                    criteria.cardType(), maskedCardNo, PG_PROVIDER);
        }
        return paymentService.retryPayment(
                criteria.orderId(), criteria.cardType(), maskedCardNo, PG_PROVIDER);
    }

    @Transactional
    public void savePaymentKey(Long orderId, String transactionKey) {
        paymentService.findByOrderId(orderId)
                .ifPresent(payment -> {
                    if (!payment.getTransactions().isEmpty()) {
                        payment.getTransactions()
                                .get(payment.getTransactions().size() - 1)
                                .assignPaymentKey(transactionKey);
                    }
                });
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 4) {
            return cardNo;
        }
        return "****-****-****-" + cardNo.substring(cardNo.length() - 4);
    }
}
