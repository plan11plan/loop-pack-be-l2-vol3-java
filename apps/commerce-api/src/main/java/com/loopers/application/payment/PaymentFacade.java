package com.loopers.application.payment;

import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private static final String CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PgPaymentClient pgPaymentClient;
    private final PaymentTransactionService paymentTransactionService;

    public PaymentResult requestPayment(Long userId, PaymentCriteria.Create criteria) {
        // TX-B: Payment 생성 (또는 재시도) — 별도 Bean이므로 TX 정상 적용
        PaymentModel payment = paymentTransactionService.createOrRetryPayment(criteria);

        // TX 밖: PG 호출
        PgPaymentResult pgResult = pgPaymentClient.requestPayment(
                new PgPaymentRequest(
                        String.format("%06d", payment.getOrderId()),
                        criteria.cardType().name(),
                        criteria.cardNo(),
                        payment.getAmount(),
                        CALLBACK_URL,
                        String.valueOf(userId)));

        // TX: paymentKey 저장
        if (pgResult.requested()) {
            paymentTransactionService.savePaymentKey(
                    payment.getOrderId(), pgResult.transactionKey());
        }

        return PaymentResult.from(payment);
    }

    public void handleCallback(String transactionKey, String pgStatus,
                               String failureCode, String failureMessage) {
        // TX-C: Payment 도메인 갱신
        PaymentModel payment = paymentService.handleCallback(
                transactionKey, pgStatus, failureCode, failureMessage);

        // TX-D: Order 도메인 갱신
        if (payment.isApproved()) {
            try {
                orderService.completeOrder(payment.getOrderId());
            } catch (Exception e) {
                log.warn("Order 상태 갱신 실패. 폴링 배치가 복구 예정. orderId={}",
                        payment.getOrderId(), e);
            }
        }
    }
}
