package com.loopers.application.payment;

import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentErrorCode;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.support.error.CoreException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private static final String PG_PROVIDER = "PG_SIMULATOR";
    private static final String CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;

    @Transactional
    public PaymentResult requestPayment(Long userId, PaymentCriteria.Create criteria) {
        OrderModel order = orderService.getByIdWithLock(criteria.orderId());
        if (!order.isPendingPayment()) {
            throw new CoreException(PaymentErrorCode.INVALID_ORDER_STATUS);
        }

        String maskedCardNo = maskCardNo(criteria.cardNo());
        Optional<PaymentModel> existing = paymentService.findByOrderId(criteria.orderId());

        PaymentModel payment;
        if (existing.isEmpty()) {
            payment = paymentService.createPayment(
                    criteria.orderId(), order.getTotalPrice(),
                    criteria.cardType(), maskedCardNo, PG_PROVIDER);
        } else {
            payment = paymentService.retryPayment(
                    criteria.orderId(), criteria.cardType(), maskedCardNo, PG_PROVIDER);
        }

        PgPaymentResult pgResult = pgPaymentClient.requestPayment(
                new PgPaymentRequest(
                        String.valueOf(payment.getOrderId()),
                        criteria.cardType().name(),
                        criteria.cardNo(),
                        payment.getAmount(),
                        CALLBACK_URL,
                        String.valueOf(userId)));

        if (pgResult.requested() && !payment.getTransactions().isEmpty()) {
            payment.getTransactions()
                    .get(payment.getTransactions().size() - 1)
                    .assignPaymentKey(pgResult.transactionKey());
        }

        return PaymentResult.from(payment);
    }

    public void handleCallback(String transactionKey, String pgStatus,
                               String failureCode, String failureMessage) {
        PaymentModel payment = paymentService.handleCallback(
                transactionKey, pgStatus, failureCode, failureMessage);

        if (payment.isApproved()) {
            try {
                orderService.completeOrder(payment.getOrderId());
            } catch (Exception e) {
                log.warn("Order 상태 갱신 실패. 폴링 배치가 복구 예정. orderId={}",
                        payment.getOrderId(), e);
            }
        }
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 4) {
            return cardNo;
        }
        return "****-****-****-" + cardNo.substring(cardNo.length() - 4);
    }
}
