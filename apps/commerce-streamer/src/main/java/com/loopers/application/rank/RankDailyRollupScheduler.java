package com.loopers.application.rank;

import com.loopers.domain.metrics.ProductMetricsDailyEntity;
import com.loopers.domain.metrics.ProductMetricsHourlyEntity;
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository;
import com.loopers.infrastructure.metrics.ProductMetricsHourlyJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankDailyRollupScheduler {

    private final ProductMetricsHourlyJpaRepository hourlyRepository;
    private final ProductMetricsDailyJpaRepository dailyRepository;

    @Scheduled(cron = "0 10 0 * * *")
    public void rollupDaily() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<ProductMetricsHourlyEntity> hourlyMetrics =
                hourlyRepository.findAllByMetricDate(yesterday);
        if (hourlyMetrics.isEmpty()) {
            log.info("[Ranking] daily 롤업 스킵 — {} 데이터 없음", yesterday);
            return;
        }

        Map<Long, List<ProductMetricsHourlyEntity>> byProduct = hourlyMetrics.stream()
                .collect(Collectors.groupingBy(ProductMetricsHourlyEntity::getProductId));

        int savedCount = 0;
        for (Map.Entry<Long, List<ProductMetricsHourlyEntity>> entry : byProduct.entrySet()) {
            Long productId = entry.getKey();
            if (dailyRepository.findByProductIdAndMetricDate(productId, yesterday).isPresent()) {
                continue;
            }
            List<ProductMetricsHourlyEntity> hourlyList = entry.getValue();
            long totalViews = hourlyList.stream().mapToLong(ProductMetricsHourlyEntity::getViewCount).sum();
            long totalLikes = hourlyList.stream().mapToLong(ProductMetricsHourlyEntity::getLikeCount).sum();
            long totalOrders = hourlyList.stream().mapToLong(ProductMetricsHourlyEntity::getOrderCount).sum();

            dailyRepository.save(ProductMetricsDailyEntity.fromHourlyAggregation(
                    productId, yesterday, totalViews, totalLikes, totalOrders));
            savedCount++;
        }

        log.info("[Ranking] daily 롤업 완료 — {} ({}건)", yesterday, savedCount);
    }
}
