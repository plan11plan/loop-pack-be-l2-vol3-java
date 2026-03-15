package com.loopers.application.payment;

import static org.assertj.core.api.Assertions.assertThat;
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
