package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsDailyEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMetricsDailyJpaRepository
        extends JpaRepository<ProductMetricsDailyEntity, Long> {

    Optional<ProductMetricsDailyEntity> findByProductIdAndMetricDate(
            Long productId, LocalDate metricDate);

    List<ProductMetricsDailyEntity> findAllByMetricDate(LocalDate metricDate);

    List<ProductMetricsDailyEntity> findAllByMetricDateBetween(LocalDate start, LocalDate end);
}
