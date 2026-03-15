package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.PgPaymentClient;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.support.error.CoreException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
    @InjectMocks PaymentFacade paymentFacade;

    @DisplayName("결제를 요청할 때, ")
    @Nested
    class RequestPayment {

        @DisplayName("최초 결제면 Payment를 생성하고 PG를 호출한다.")
        @Test
        void requestPayment_firstTime_createsPaymentAndCallsPg() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(any()))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(true, "20260316:TR:abc", "PENDING"));

            // act
            PaymentResult result = paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, CardType.SAMSUNG, "1234-5678-9814-1451"));

            // assert
            assertAll(
                    () -> assertThat(result.paymentId()).isEqualTo(1L),
                    () -> assertThat(result.status()).isEqualTo("PENDING"));
            verify(paymentTransactionService).createOrRetryPayment(any());
            verify(pgPaymentClient).requestPayment(any(PgPaymentRequest.class));
            verify(paymentTransactionService).savePaymentKey(1L, "20260316:TR:abc");
        }

        @DisplayName("PG 서버가 불안정하면 PG_SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        void requestPayment_whenPgUnavailable_throwsException() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(any()))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(false, null, "PG_UNAVAILABLE",
                            "결제 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, CardType.SAMSUNG, "1234-5678-9814-1451")))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 서비스가 일시적으로 불안정합니다");
            verify(paymentTransactionService).failLastTransaction(
                    eq(1L), eq("PG_REQUEST_FAILED"), anyString());
        }

        @DisplayName("PG 요청 파라미터가 잘못되면 PG_REQUEST_FAILED 예외가 발생한다.")
        @Test
        void requestPayment_whenPgBadRequest_throwsException() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(any()))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(false, null, "PG_BAD_REQUEST",
                            "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다."));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, CardType.SAMSUNG, "invalid")))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다");
            verify(paymentTransactionService).failLastTransaction(
                    eq(1L), eq("PG_REQUEST_FAILED"), anyString());
        }

        @DisplayName("PG 연결이 실패하면 PG_SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        void requestPayment_whenPgConnectionFailed_throwsException() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentTransactionService.createOrRetryPayment(any()))
                    .thenReturn(payment);
            when(pgPaymentClient.requestPayment(any(PgPaymentRequest.class)))
                    .thenReturn(new PgPaymentResult(false, null, "PG_UNAVAILABLE",
                            "결제 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요."));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(
                    100L,
                    new PaymentCriteria.Create(1L, CardType.SAMSUNG, "1234-5678-9814-1451")))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("결제 서비스에 연결할 수 없습니다");
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
            when(paymentService.handleCallback(
                    eq("PK_001"), eq("SUCCESS"), any(), any()))
                    .thenReturn(payment);

            // act
            paymentFacade.handleCallback("PK_001", "SUCCESS", null, null);

            // assert
            verify(orderService).completeOrder(1L);
        }

        @DisplayName("FAILED 콜백이면 Order를 변경하지 않는다.")
        @Test
        void handleCallback_failed_doesNotChangeOrder() {
            // arrange
            PaymentModel payment = mockPayment(1L, 1L, 50000, PaymentStatus.PENDING);
            when(paymentService.handleCallback(
                    eq("PK_002"), eq("FAILED"), eq("LIMIT_EXCEEDED"), anyString()))
                    .thenReturn(payment);

            // act
            paymentFacade.handleCallback(
                    "PK_002", "FAILED", "LIMIT_EXCEEDED", "한도초과");

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
        lenient().when(payment.getTransactions()).thenReturn(new ArrayList<>());
        return payment;
    }
}
