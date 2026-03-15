package com.loopers.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PaymentModelTest {

    @DisplayName("결제를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("모든 필드가 주어지면 PENDING 상태로 생성된다.")
        @Test
        void create_whenAllDataProvided() {
            // act
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");

            // assert
            assertAll(
                    () -> assertThat(payment.getOrderId()).isEqualTo(1L),
                    () -> assertThat(payment.getAmount()).isEqualTo(50000),
                    () -> assertThat(payment.getCardType()).isEqualTo(CardType.SAMSUNG),
                    () -> assertThat(payment.getMaskedCardNo()).isEqualTo("****-****-****-1451"),
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING),
                    () -> assertThat(payment.getApprovedAt()).isNull(),
                    () -> assertThat(payment.getTransactions()).isEmpty());
        }
    }

    @DisplayName("결제를 승인할 때, ")
    @Nested
    class Approve {

        @DisplayName("상태가 APPROVED로 전이되고 승인 시각이 기록된다.")
        @Test
        void approve_setsStatusAndApprovedAt() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");
            LocalDateTime approvedAt = LocalDateTime.of(2026, 3, 16, 10, 30);

            // act
            payment.approve(approvedAt);

            // assert
            assertAll(
                    () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED),
                    () -> assertThat(payment.getApprovedAt()).isEqualTo(approvedAt));
        }
    }

    @DisplayName("결제를 실패 처리할 때, ")
    @Nested
    class Fail {

        @DisplayName("상태가 FAILED로 전이된다.")
        @Test
        void fail_setsStatusToFailed() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");

            // act
            payment.fail();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @DisplayName("카드 정보를 갱신할 때, ")
    @Nested
    class UpdateCardInfo {

        @DisplayName("카드 종류와 마스킹 번호가 변경된다.")
        @Test
        void updateCardInfo_changesCardTypeAndNo() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");

            // act
            payment.updateCardInfo(CardType.KB, "****-****-****-9999");

            // assert
            assertAll(
                    () -> assertThat(payment.getCardType()).isEqualTo(CardType.KB),
                    () -> assertThat(payment.getMaskedCardNo())
                            .isEqualTo("****-****-****-9999"));
        }
    }

    @DisplayName("재시도 가능 여부를 확인할 때, ")
    @Nested
    class IsRetryable {

        @DisplayName("PENDING이고 PROCESSING TX가 없으면 재시도 가능하다.")
        @Test
        void isRetryable_whenPendingAndNoProcessingTx() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");

            // assert
            assertThat(payment.isRetryable()).isTrue();
        }

        @DisplayName("PROCESSING TX가 있으면 재시도 불가하다.")
        @Test
        void isRetryable_whenProcessingTxExists_returnsFalse() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");
            payment.addTransaction(PaymentTransactionModel.create(
                    payment, "PG_SIMULATOR", LocalDateTime.now()));

            // assert
            assertThat(payment.isRetryable()).isFalse();
        }

        @DisplayName("APPROVED 상태면 재시도 불가하다.")
        @Test
        void isRetryable_whenApproved_returnsFalse() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");
            payment.approve(LocalDateTime.now());

            // assert
            assertThat(payment.isRetryable()).isFalse();
        }
    }

    @DisplayName("Transaction을 추가할 때, ")
    @Nested
    class AddTransaction {

        @DisplayName("양방향 관계가 설정된다.")
        @Test
        void addTransaction_setsBidirectionalRelation() {
            // arrange
            PaymentModel payment = PaymentModel.create(
                    1L, 50000, CardType.SAMSUNG, "****-****-****-1451");
            PaymentTransactionModel tx = PaymentTransactionModel.create(
                    payment, "PG_SIMULATOR", LocalDateTime.now());

            // act
            payment.addTransaction(tx);

            // assert
            assertAll(
                    () -> assertThat(payment.getTransactions()).hasSize(1),
                    () -> assertThat(payment.getTransactions().get(0)).isEqualTo(tx),
                    () -> assertThat(tx.getPayment()).isEqualTo(payment));
        }
    }
}
