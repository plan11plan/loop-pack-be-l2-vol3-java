package com.loopers.application.rank;

import com.loopers.domain.metrics.ProductMetricsHourlyEntity;
import com.loopers.domain.rank.RankWeightProperties;
import com.loopers.domain.rank.RankWeightVersion;
import com.loopers.infrastructure.metrics.ProductMetricsHourlyJpaRepository;
import com.loopers.infrastructure.rank.RankRedisUpdater;
import java.time.LocalDate;
import java.util.HashMap;
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
public class RankHourlyAggregationScheduler {

    private final ProductMetricsHourlyJpaRepository hourlyRepository;
    private final RankWeightProperties weightProperties;
    private final RankRedisUpdater rankRedisUpdater;

    @Scheduled(cron = "0 5 * * * *")
    public void aggregateHourly() {
        LocalDate today = LocalDate.now();
        List<ProductMetricsHourlyEntity> hourlyMetrics =
                hourlyRepository.findAllByMetricDate(today);
        if (hourlyMetrics.isEmpty()) {
            log.info("[Ranking] hourly 집계 스킵 — {} 데이터 없음", today);
            return;
        }

        Map<Long, long[]> aggregated = aggregateByProduct(hourlyMetrics);

        for (RankWeightVersion version : weightProperties.getVersions()) {
            Map<Long, Double> scores = new HashMap<>();
            aggregated.forEach((productId, counts) ->
                    scores.put(productId,
                            version.computeScore(counts[0], counts[1], counts[2])));
            rankRedisUpdater.replaceScores(version.versionKey(), today, scores);
        }

        log.info("[Ranking] hourly 집계 완료 — {} ({}개 상품, {}개 버전)",
                today, aggregated.size(), weightProperties.getVersions().size());
    }

    private Map<Long, long[]> aggregateByProduct(List<ProductMetricsHourlyEntity> metrics) {
        Map<Long, List<ProductMetricsHourlyEntity>> byProduct = metrics.stream()
                .collect(Collectors.groupingBy(ProductMetricsHourlyEntity::getProductId));

        Map<Long, long[]> result = new HashMap<>();
        byProduct.forEach((productId, list) -> {
            long views = list.stream().mapToLong(ProductMetricsHourlyEntity::getViewCount).sum();
            long likes = list.stream().mapToLong(ProductMetricsHourlyEntity::getLikeCount).sum();
            long orders = list.stream().mapToLong(ProductMetricsHourlyEntity::getOrderCount).sum();
            result.put(productId, new long[]{views, likes, orders});
        });
        return result;
    }
}
