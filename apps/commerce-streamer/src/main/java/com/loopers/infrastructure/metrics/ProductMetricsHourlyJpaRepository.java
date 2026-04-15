package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsHourlyEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductMetricsHourlyJpaRepository
        extends JpaRepository<ProductMetricsHourlyEntity, Long> {

    Optional<ProductMetricsHourlyEntity> findByProductIdAndMetricDateAndMetricHour(
            Long productId, LocalDate metricDate, int metricHour);

    List<ProductMetricsHourlyEntity> findAllByMetricDateAndMetricHour(
            LocalDate metricDate, int metricHour);

    List<ProductMetricsHourlyEntity> findAllByMetricDate(LocalDate metricDate);

    @Modifying
    @Query(value = "INSERT INTO product_metrics_hourly (product_id, metric_date, metric_hour, view_count, like_count, order_count, updated_at) "
            + "VALUES (:productId, :metricDate, :metricHour, :viewDelta, :likeDelta, :orderDelta, NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "view_count = view_count + :viewDelta, "
            + "like_count = like_count + :likeDelta, "
            + "order_count = order_count + :orderDelta, "
            + "updated_at = NOW()", nativeQuery = true)
    void upsertMetrics(Long productId, LocalDate metricDate, int metricHour,
            long viewDelta, long likeDelta, long orderDelta);
}
