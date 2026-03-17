package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderErrorCode;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.dto.OrderInfo;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.PgCallbackStatus;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.domain.payment.PgRequestStatus;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock OrderService orderService;
    @Mock PaymentService paymentService;
    @Mock PgPaymentClient pgPaymentClient;
    @Mock PaymentTransactionService paymentTransactionService;
    @Mock ProductService productService;
    @Mock UserService userService;
    @Mock CouponService couponService;
    @InjectMocks PaymentFacade paymentFacade;

    @DisplayName("결제를 요청할 때, ")
    @Nested
    class RequestPayment {

        @DisplayName("새 주문 + 결제면 주문과 Payment를 함께 생성하고 포인트 차감 후 PG를 호출한다.")
        @Test
        void requestPayment_newOrder_createsOrderAndPaymentThenCallsPg() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrderAndPayment(eq(100L), any()))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(true, "20260316:TR:abc", PgRequestStatus.ACCEPTED));

            // act
            PaymentResult result = paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(
                            null,
                            List.of(new PaymentCriteria.Create.OrderItem(10L, 2, 25000)),
                            null,
                            CardType.SAMSUNG,
                            "1234-5678-9814-1451"));

            // assert
            assertAll(
                    () -> assertThat(result.paymentId()).isEqualTo(1L),
                    () -> assertThat(result.status()).isEqualTo("PENDING"));
            verify(paymentTransactionService).createOrderAndPayment(eq(100L), any());
            verify(userService).deductPoint(100L, 50000);
            verify(pgPaymentClient).requestPayment(any(PgPaymentRequest.class));
            verify(paymentTransactionService).savePaymentKey(1L, "20260316:TR:abc");
            verify(userService, never()).addPoint(anyLong(), any(Integer.class));
        }

        @DisplayName("기존 주문 재시도면 Payment를 생성하고 포인트 차감 후 PG를 호출한다.")
        @Test
        void requestPayment_retry_createsPaymentAndCallsPg() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(1L, CardType.SAMSUNG, "1234-5678-9814-1451"))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(true, "20260316:TR:abc", PgRequestStatus.ACCEPTED));

            // act
            PaymentResult result = paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, null, null, CardType.SAMSUNG, "1234-5678-9814-1451"));

            // assert
            assertAll(
                    () -> assertThat(result.paymentId()).isEqualTo(1L),
                    () -> assertThat(result.status()).isEqualTo("PENDING"));
            verify(paymentTransactionService).createOrRetryPayment(1L, CardType.SAMSUNG, "1234-5678-9814-1451");
            verify(userService).deductPoint(100L, 50000);
            verify(pgPaymentClient).requestPayment(any(PgPaymentRequest.class));
            verify(paymentTransactionService).savePaymentKey(1L, "20260316:TR:abc");
        }

        @DisplayName("PG 서버가 불안정하면 포인트 복원 후 PG_SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        void requestPayment_whenPgUnavailable_throwsException() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(1L, CardType.SAMSUNG, "1234-5678-9814-1451"))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(false, null, PgRequestStatus.SERVER_ERROR,
                            "현재 서버가 불안정합니다."));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, null, null, CardType.SAMSUNG, "1234-5678-9814-1451")))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 서비스가 일시적으로 지연되고 있습니다");
            verify(userService).deductPoint(100L, 50000);
            verify(userService).addPoint(100L, 50000);
            verify(paymentTransactionService).failLastTransaction(
                    eq(1L), eq("PG_REQUEST_FAILED"), anyString());
        }

        @DisplayName("PG 요청 파라미터가 잘못되면 포인트 복원 후 PG_REQUEST_FAILED 예외가 발생한다.")
        @Test
        void requestPayment_whenPgBadRequest_throwsException() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(1L, CardType.SAMSUNG, "invalid"))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(false, null, PgRequestStatus.VALIDATION_ERROR,
                            "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다."));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, null, null, CardType.SAMSUNG, "invalid")))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다");
            verify(userService).deductPoint(100L, 50000);
            verify(userService).addPoint(100L, 50000);
            verify(paymentTransactionService).failLastTransaction(
                    eq(1L), eq("PG_REQUEST_FAILED"), anyString());
        }

        @DisplayName("PG 연결이 실패하면 포인트 복원 후 PG_SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        void requestPayment_whenPgConnectionFailed_throwsException() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(1L, CardType.SAMSUNG, "1234-5678-9814-1451"))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(false, null, PgRequestStatus.CONNECTION_ERROR,
                            "Connection refused"));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, null, null, CardType.SAMSUNG, "1234-5678-9814-1451")))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 서비스가 일시적으로 지연되고 있습니다");
            verify(userService).deductPoint(100L, 50000);
            verify(userService).addPoint(100L, 50000);
        }
    }

    @DisplayName("콜백을 처리할 때, ")
    @Nested
    class HandleCallback {

        @DisplayName("SUCCESS 콜백이면 Payment 승인 후 Order를 완료한다.")
        @Test
        void handleCallback_success_approvesAndCompletesOrder() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.APPROVED);
            when(pgPaymentClient.resolveCallbackStatus("SUCCESS", "정상 승인되었습니다."))
                    .thenReturn(PgCallbackStatus.APPROVED);
            when(paymentService.handleCallback(
                    eq("PK_001"), eq(PgCallbackStatus.APPROVED), any()))
                    .thenReturn(payment);

            // act
            paymentFacade.handleCallback("PK_001", "SUCCESS", "정상 승인되었습니다.");

            // assert
            verify(orderService).completeOrder(1L);
        }

        @DisplayName("FAILED 콜백이면 주문 취소 + 재고 복구 + 포인트 반환 + 쿠폰 복원한다.")
        @Test
        void handleCallback_failed_compensatesResources() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.FAILED);
            when(pgPaymentClient.resolveCallbackStatus("FAILED", "한도초과"))
                    .thenReturn(PgCallbackStatus.LIMIT_EXCEEDED);
            when(paymentService.handleCallback(
                    eq("PK_002"), eq(PgCallbackStatus.LIMIT_EXCEEDED), anyString()))
                    .thenReturn(payment);

            OrderInfo.PaymentFailureCancellation cancellation =
                    new OrderInfo.PaymentFailureCancellation(100L, 50000, List.of(
                            new OrderInfo.PaymentFailureCancellation.CancelledItem(10L, 2),
                            new OrderInfo.PaymentFailureCancellation.CancelledItem(20L, 1)));
            when(orderService.cancelByPaymentFailure(1L)).thenReturn(cancellation);

            // act
            paymentFacade.handleCallback("PK_002", "FAILED", "한도초과");

            // assert
            assertAll(
                    () -> verify(orderService).cancelByPaymentFailure(1L),
                    () -> verify(productService).increaseStock(10L, 2),
                    () -> verify(productService).increaseStock(20L, 1),
                    () -> verify(userService).addPoint(100L, 50000),
                    () -> verify(couponService).restoreByOrderId(1L));
        }

        @DisplayName("SUCCESS 콜백이지만 주문이 이미 취소됐으면 PG 환불을 요청한다.")
        @Test
        void handleCallback_success_butOrderCancelled_refundsToPg() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.APPROVED);
            when(pgPaymentClient.resolveCallbackStatus("SUCCESS", "정상 승인되었습니다."))
                    .thenReturn(PgCallbackStatus.APPROVED);
            when(paymentService.handleCallback(
                    eq("PK_LATE"), eq(PgCallbackStatus.APPROVED), any()))
                    .thenReturn(payment);
            org.mockito.Mockito.doThrow(new CoreException(OrderErrorCode.INVALID_STATUS_TRANSITION))
                    .when(orderService).completeOrder(1L);

            OrderModel cancelledOrder = org.mockito.Mockito.mock(OrderModel.class);
            lenient().when(cancelledOrder.isCancelled()).thenReturn(true);
            when(orderService.getById(1L)).thenReturn(cancelledOrder);

            // act
            paymentFacade.handleCallback("PK_LATE", "SUCCESS", "정상 승인되었습니다.");

            // assert
            verify(pgPaymentClient).refund("PK_LATE");
        }

        @DisplayName("FAILED 콜백이면 Order를 변경하지 않는다.")
        @Test
        void handleCallback_failed_doesNotChangeOrder() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(pgPaymentClient.resolveCallbackStatus("FAILED", "한도초과"))
                    .thenReturn(PgCallbackStatus.LIMIT_EXCEEDED);
            when(paymentService.handleCallback(
                    eq("PK_002"), eq(PgCallbackStatus.LIMIT_EXCEEDED), anyString()))
                    .thenReturn(payment);

            // act
            paymentFacade.handleCallback("PK_002", "FAILED", "한도초과");

            // assert
            verify(orderService, never()).completeOrder(any());
        }
    }

    private PaymentModel mockPayment(Long paymentId, Long orderId,
                                     int amount, PaymentStatus status) {
        PaymentModel payment = org.mockito.Mockito.mock(PaymentModel.class);
        lenient().when(payment.getId()).thenReturn(paymentId);
        lenient().when(payment.getOrderId()).thenReturn(orderId);
        lenient().when(payment.getAmount()).thenReturn(amount);
        lenient().when(payment.getStatus()).thenReturn(status);
        lenient().when(payment.getCardType()).thenReturn(CardType.SAMSUNG);
        lenient().when(payment.getMaskedCardNo()).thenReturn("****-****-****-1451");
        lenient().when(payment.getCreatedAt()).thenReturn(ZonedDateTime.now());
        lenient().when(payment.isApproved()).thenReturn(status == PaymentStatus.APPROVED);
        lenient().when(payment.isFailed()).thenReturn(status == PaymentStatus.FAILED);
        lenient().when(payment.getTransactions()).thenReturn(new ArrayList<>());
        return payment;
    }
}
