package com.loopers.domain.rank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_product_rank_monthly",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_monthly_product_period",
                        columnNames = {"product_id", "period_month"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRankMonthlyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "rank_value", nullable = false)
    private int rankValue;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "order_count", nullable = false)
    private long orderCount;

    @Column(name = "period_month", nullable = false, length = 10)
    private String yearMonth;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.updatedAt = ZonedDateTime.now();
    }

    // === 생성 === //

    private ProductRankMonthlyEntity(Long productId, int rankValue, double score,
            long viewCount, long likeCount, long orderCount, String yearMonth) {
        this.productId = productId;
        this.rankValue = rankValue;
        this.score = score;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
        this.yearMonth = yearMonth;
    }

    public static ProductRankMonthlyEntity of(int rankValue, Long productId, double score,
            long viewCount, long likeCount, long orderCount, String yearMonth) {
        return new ProductRankMonthlyEntity(
                productId, rankValue, score, viewCount, likeCount, orderCount, yearMonth);
    }
}
