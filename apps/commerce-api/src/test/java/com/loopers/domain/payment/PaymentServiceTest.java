package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

    private PaymentService paymentService;
    private FakePaymentRepository paymentRepository;
    private FakePaymentTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        paymentRepository = new FakePaymentRepository();
        transactionRepository = new FakePaymentTransactionRepository();
        paymentService = new PaymentService(paymentRepository, transactionRepository);
    }

    @DisplayName("결제를 생성할 때, ")
    @Nested
    class CreatePayment {

        @DisplayName("Payment와 Transaction이 함께 생성된다.")
        @Test
        void createPayment_createsPaymentAndTransaction() {
            // act
            PaymentModel payment = paymentService.createPayment(
                    1L, 50000, CardType.SAMSUNG,
                    "****-****-****-1451", "PG_SIMULATOR");

            // assert
            assertAll(
                    () -> assertThat(payment.getId()).isNotNull(),
                    () -> assertThat(payment.getOrderId()).isEqualTo(1L),
                    () -> assertThat(payment.getAmount()).isEqualTo(50000),
                    () -> assertThat(payment.getCardType()).isEqualTo(CardType.SAMSUNG),
                    () -> assertThat(payment.getMaskedCardNo()).isEqualTo("****-****-****-1451"),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING),
                    () -> assertThat(payment.getTransactions()).hasSize(1),
                    () -> assertThat(payment.getTransactions().get(0).getStatus())
                            .isEqualTo(TransactionStatus.PROCESSING));
        }
    }

    @DisplayName("결제를 재시도할 때, ")
    @Nested
    class RetryPayment {

        @DisplayName("카드 정보를 갱신하고 새 Transaction을 생성한다.")
        @Test
        void retryPayment_updatesCardAndCreatesNewTransaction() {
            // arrange
            PaymentModel payment = paymentService.createPayment(
                    1L, 50000, CardType.SAMSUNG,
                    "****-****-****-1451", "PG_SIMULATOR");
            failFirstTransaction(payment);

            // act
            paymentService.retryPayment(payment.getOrderId(),
                    CardType.KB, "****-****-****-9999", "PG_SIMULATOR");

            // assert
            assertAll(
                    () -> assertThat(payment.getCardType()).isEqualTo(CardType.KB),
                    () -> assertThat(payment.getMaskedCardNo())
                            .isEqualTo("****-****-****-9999"),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING),
                    () -> assertThat(payment.getTransactions()).hasSize(2),
                    () -> assertThat(payment.getTransactions().get(1).getStatus())
                            .isEqualTo(TransactionStatus.PROCESSING));
        }

        @DisplayName("PROCESSING 상태 Transaction이 있으면 예외가 발생한다.")
        @Test
        void retryPayment_whenProcessingTxExists_throwsException() {
            // arrange — Transaction은 PROCESSING 상태
            paymentService.createPayment(
                    1L, 50000, CardType.SAMSUNG,
                    "****-****-****-1451", "PG_SIMULATOR");

            // act & assert
            assertThatThrownBy(() -> paymentService.retryPayment(
                    1L, CardType.KB, "****-****-****-9999", "PG_SIMULATOR"))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("콜백을 처리할 때, ")
    @Nested
    class HandleCallback {

        @DisplayName("SUCCESS 콜백이면 Transaction과 Payment를 승인한다.")
        @Test
        void handleCallback_whenSucceeded_approvesPayment() {
            // arrange
            PaymentModel payment = createPaymentWithPaymentKey("PK_001");

            // act
            paymentService.handleCallback("PK_001", "SUCCESS", null, null);

            // assert
            PaymentTransactionModel tx = payment.getTransactions().get(0);
            assertAll(
                    () -> assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED),
                    () -> assertThat(payment.getApprovedAt()).isNotNull());
        }

        @DisplayName("FAILED 콜백이면 Transaction만 실패하고 Payment는 PENDING 유지한다.")
        @Test
        void handleCallback_whenFailed_keepsPaymentPending() {
            // arrange
            PaymentModel payment = createPaymentWithPaymentKey("PK_002");

            // act
            paymentService.handleCallback(
                    "PK_002", "FAILED", "LIMIT_EXCEEDED", "한도초과입니다.");

            // assert
            PaymentTransactionModel tx = payment.getTransactions().get(0);
            assertAll(
                    () -> assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED),
                    () -> assertThat(tx.getFailureCode()).isEqualTo("LIMIT_EXCEEDED"),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING));
        }

        @DisplayName("이미 APPROVED인 Payment에 중복 콜백이 오면 무시한다.")
        @Test
        void handleCallback_whenAlreadyApproved_ignores() {
            // arrange
            PaymentModel payment = createPaymentWithPaymentKey("PK_003");
            paymentService.handleCallback("PK_003", "SUCCESS", null, null);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

            // act — 동일 paymentKey로 2번째 콜백
            PaymentModel result = paymentService.handleCallback(
                    "PK_003", "SUCCESS", null, null);

            // assert — 상태 변경 없이 그대로 반환
            assertThat(result.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        }
    }

    @DisplayName("결제를 실패 처리할 때, ")
    @Nested
    class FailPayment {

        @DisplayName("타임아웃 시 Payment를 FAILED로 확정한다.")
        @Test
        void failPayment_setsStatusToFailed() {
            // arrange
            PaymentModel payment = paymentService.createPayment(
                    4L, 60000, CardType.LOTTE,
                    "****-****-****-4444", "PG_SIMULATOR");

            // act
            paymentService.failPayment(payment.getOrderId());

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @DisplayName("결제를 조회할 때, ")
    @Nested
    class FindPayment {

        @DisplayName("orderId로 Payment를 조회한다.")
        @Test
        void findByOrderId_returnsPayment() {
            // arrange
            paymentService.createPayment(
                    5L, 70000, CardType.HYUNDAI,
                    "****-****-****-5555", "PG_SIMULATOR");

            // act & assert
            assertThat(paymentService.findByOrderId(5L)).isPresent();
        }

        @DisplayName("존재하지 않는 orderId면 빈 결과를 반환한다.")
        @Test
        void findByOrderId_whenNotExists_returnsEmpty() {
            assertThat(paymentService.findByOrderId(999L)).isEmpty();
        }
    }

    // === 헬퍼 메서드 === //

    private PaymentModel createPaymentWithPaymentKey(String paymentKey) {
        PaymentModel payment = paymentService.createPayment(
                System.nanoTime(), 50000, CardType.SAMSUNG,
                "****-****-****-1451", "PG_SIMULATOR");
        payment.getTransactions().get(0).assignPaymentKey(paymentKey);
        return payment;
    }

    private void failFirstTransaction(PaymentModel payment) {
        payment.getTransactions().get(0)
                .fail("LIMIT_EXCEEDED", "한도초과입니다.");
    }
}
