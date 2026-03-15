package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentModel extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "masked_card_no", nullable = false)
    private String maskedCardNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Version
    private Long version;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "payment", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<PaymentTransactionModel> transactions = new ArrayList<>();

    private PaymentModel(Long orderId, int amount, CardType cardType,
                         String maskedCardNo, PaymentStatus status) {
        this.orderId = orderId;
        this.amount = amount;
        this.cardType = cardType;
        this.maskedCardNo = maskedCardNo;
        this.status = status;
    }

    public static PaymentModel create(Long orderId, int amount,
                                      CardType cardType, String maskedCardNo) {
        return new PaymentModel(orderId, amount, cardType,
                maskedCardNo, PaymentStatus.PENDING);
    }

    // === 도메인 로직 === //

    public void approve(LocalDateTime approvedAt) {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt;
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
    }

    public void updateCardInfo(CardType cardType, String maskedCardNo) {
        this.cardType = cardType;
        this.maskedCardNo = maskedCardNo;
    }

    public void addTransaction(PaymentTransactionModel transaction) {
        transactions.add(transaction);
        transaction.assignPayment(this);
    }

    public boolean hasProcessingTransaction() {
        return transactions.stream()
                .anyMatch(tx -> tx.getStatus() == TransactionStatus.PROCESSING);
    }

    public boolean isRetryable() {
        return this.status == PaymentStatus.PENDING && !hasProcessingTransaction();
    }

    public boolean isApproved() {
        return this.status == PaymentStatus.APPROVED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }
}
