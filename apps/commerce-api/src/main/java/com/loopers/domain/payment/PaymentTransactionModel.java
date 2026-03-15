package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransactionModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private PaymentModel payment;

    @Column(name = "payment_key", unique = true)
    private String paymentKey;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "pg_provider", nullable = false)
    private String pgProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "failure_code")
    private String failureCode;

    @Column(name = "failure_message")
    private String failureMessage;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    private PaymentTransactionModel(PaymentModel payment, String pgProvider,
                                    TransactionStatus status, LocalDateTime attemptedAt) {
        this.payment = payment;
        this.pgProvider = pgProvider;
        this.status = status;
        this.attemptedAt = attemptedAt;
    }

    public static PaymentTransactionModel create(PaymentModel payment,
                                                 String pgProvider,
                                                 LocalDateTime attemptedAt) {
        return new PaymentTransactionModel(
                payment, pgProvider, TransactionStatus.PROCESSING, attemptedAt);
    }

    // === 도메인 로직 === //

    public void succeed(String transactionId) {
        this.status = TransactionStatus.SUCCEEDED;
        this.transactionId = transactionId;
    }

    public void fail(String failureCode, String failureMessage) {
        this.status = TransactionStatus.FAILED;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public void assignPaymentKey(String paymentKey) {
        this.paymentKey = paymentKey;
    }

    public void assignPayment(PaymentModel payment) {
        this.payment = payment;
    }

    public boolean isProcessing() {
        return this.status == TransactionStatus.PROCESSING;
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.SUCCEEDED
                || this.status == TransactionStatus.FAILED;
    }
}
