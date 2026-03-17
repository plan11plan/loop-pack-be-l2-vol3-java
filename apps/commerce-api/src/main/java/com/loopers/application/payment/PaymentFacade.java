package com.loopers.application.payment;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.dto.OrderInfo;
import com.loopers.domain.payment.PaymentErrorCode;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PgCallbackStatus;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.domain.payment.PgRequestStatus;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
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
    private final ProductService productService;
    private final UserService userService;
    private final CouponService couponService;

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

        if (pgResult.requested()) {
            paymentTransactionService.savePaymentKey(
                    payment.getOrderId(), pgResult.transactionKey());
            return PaymentResult.from(payment);
        }

        paymentTransactionService.failLastTransaction(
                payment.getOrderId(), "PG_REQUEST_FAILED", pgResult.pgDetail());
        if (pgResult.status() == PgRequestStatus.VALIDATION_ERROR) {
            throw new CoreException(PaymentErrorCode.PG_REQUEST_FAILED, pgResult.pgDetail());
        }
        throw new CoreException(PaymentErrorCode.PG_SERVICE_UNAVAILABLE);
    }

    public PaymentStatusResult getPaymentStatus(Long orderId) {
        PaymentModel payment = paymentService.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(PaymentErrorCode.PAYMENT_NOT_FOUND));
        return PaymentStatusResult.from(payment);
    }

    public void handleCallback(String transactionKey, String pgStatus, String pgReason) {
        PgCallbackStatus callbackStatus =
                pgPaymentClient.resolveCallbackStatus(pgStatus, pgReason);

        // TX-C: Payment 도메인 갱신
        PaymentModel payment = paymentService.handleCallback(
                transactionKey, callbackStatus, pgReason);

        // TX-D: Order 도메인 갱신
        if (payment.isApproved()) {
            try {
                orderService.completeOrder(payment.getOrderId());
            } catch (Exception e) {
                log.warn("Order 상태 갱신 실패. 폴링 배치가 복구 예정. orderId={}",
                        payment.getOrderId(), e);
            }
        }

        if (payment.isFailed()) {
            OrderInfo.PaymentFailureCancellation cancellation =
                    orderService.cancelByPaymentFailure(payment.getOrderId());
            cancellation.items().forEach(item ->
                    productService.increaseStock(item.productId(), item.quantity()));
            userService.addPoint(cancellation.userId(), cancellation.totalPrice());
            couponService.restoreByOrderId(payment.getOrderId());
        }
    }
}
