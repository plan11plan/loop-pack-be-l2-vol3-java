package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PaymentTransactionModelTest {

    @DisplayName("Transaction을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("PROCESSING 상태로 생성된다.")
        @Test
        void create_setsProcessingStatus() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");
            LocalDateTime attemptedAt = LocalDateTime.of(2026, 3, 16, 10, 0);

            // act
            PaymentTransactionModel tx = PaymentTransactionModel.create(
                    payment, "PG_SIMULATOR", attemptedAt);

            // assert
            assertAll(
                    () -> assertThat(tx.getPayment()).isEqualTo(payment),
                    () -> assertThat(tx.getPgProvider()).isEqualTo("PG_SIMULATOR"),
                    () -> assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PROCESSING),
                    () -> assertThat(tx.getAttemptedAt()).isEqualTo(attemptedAt),
                    () -> assertThat(tx.getPaymentKey()).isNull(),
                    () -> assertThat(tx.getFailureCode()).isNull());
        }
    }

    @DisplayName("Transaction을 승인할 때, ")
    @Nested
    class Succeed {

        @DisplayName("상태가 SUCCEEDED로 전이된다.")
        @Test
        void succeed_setsStatusToSucceeded() {
            // arrange
            PaymentTransactionModel tx = createProcessingTx();

            // act
            tx.succeed("txId-123");

            // assert
            assertAll(
                    () -> assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED),
                    () -> assertThat(tx.getTransactionId()).isEqualTo("txId-123"));
        }
    }

    @DisplayName("Transaction을 실패 처리할 때, ")
    @Nested
    class Fail {

        @DisplayName("상태가 FAILED로 전이되고 실패 사유가 기록된다.")
        @Test
        void fail_setsStatusAndFailureInfo() {
            // arrange
            PaymentTransactionModel tx = createProcessingTx();

            // act
            tx.fail("LIMIT_EXCEEDED", "한도초과입니다.");

            // assert
            assertAll(
                    () -> assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED),
                    () -> assertThat(tx.getFailureCode()).isEqualTo("LIMIT_EXCEEDED"),
                    () -> assertThat(tx.getFailureMessage()).isEqualTo("한도초과입니다."));
        }
    }

    @DisplayName("paymentKey를 할당할 때, ")
    @Nested
    class AssignPaymentKey {

        @DisplayName("paymentKey가 설정된다.")
        @Test
        void assignPaymentKey_setsKey() {
            // arrange
            PaymentTransactionModel tx = createProcessingTx();

            // act
            tx.assignPaymentKey("20260316:TR:abc123");

            // assert
            assertThat(tx.getPaymentKey()).isEqualTo("20260316:TR:abc123");
        }
    }

    @DisplayName("상태를 확인할 때, ")
    @Nested
    class StatusCheck {

        @DisplayName("PROCESSING이면 isProcessing이 true이다.")
        @Test
        void isProcessing_whenProcessing() {
            assertThat(createProcessingTx().isProcessing()).isTrue();
        }

        @DisplayName("SUCCEEDED이면 isCompleted가 true이다.")
        @Test
        void isCompleted_whenSucceeded() {
            // arrange
            PaymentTransactionModel tx = createProcessingTx();
            tx.succeed("txId");

            // assert
            assertThat(tx.isCompleted()).isTrue();
        }

        @DisplayName("FAILED이면 isCompleted가 true이다.")
        @Test
        void isCompleted_whenFailed() {
            // arrange
            PaymentTransactionModel tx = createProcessingTx();
            tx.fail("TIMEOUT", "타임아웃");

            // assert
            assertThat(tx.isCompleted()).isTrue();
        }
    }

    private PaymentTransactionModel createProcessingTx() {
        PaymentModel payment = PaymentModel.create(
                1L, 50000, CardType.SAMSUNG, "****-****-****-1451");
        return PaymentTransactionModel.create(
                payment, "PG_SIMULATOR", LocalDateTime.now());
    }
}
