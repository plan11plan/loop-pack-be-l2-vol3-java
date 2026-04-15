package com.loopers.domain.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_metrics_daily",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_metrics_daily_product_date",
                        columnNames = {"product_id", "metric_date"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsDailyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "order_count", nullable = false)
    private long orderCount;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        this.updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    // === 생성 === //

    private ProductMetricsDailyEntity(Long productId, LocalDate metricDate,
            long viewCount, long likeCount, long orderCount) {
        this.productId = productId;
        this.metricDate = metricDate;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.orderCount = orderCount;
    }

    public static ProductMetricsDailyEntity fromHourlyAggregation(
            Long productId, LocalDate date,
            long views, long likes, long orders) {
        return new ProductMetricsDailyEntity(productId, date, views, likes, orders);
    }
}
