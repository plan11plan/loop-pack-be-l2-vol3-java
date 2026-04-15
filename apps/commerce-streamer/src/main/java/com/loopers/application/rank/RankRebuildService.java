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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankRebuildService {

    private final ProductMetricsHourlyJpaRepository hourlyRepository;
    private final RankWeightProperties weightProperties;
    private final RankRedisUpdater rankRedisUpdater;

    public void rebuildForDate(LocalDate date) {
        List<ProductMetricsHourlyEntity> hourlyMetrics =
                hourlyRepository.findAllByMetricDate(date);
        if (hourlyMetrics.isEmpty()) {
            log.info("[Ranking] rebuild 스킵 — {} 데이터 없음", date);
            return;
        }

        Map<Long, List<ProductMetricsHourlyEntity>> byProduct = hourlyMetrics.stream()
                .collect(Collectors.groupingBy(ProductMetricsHourlyEntity::getProductId));

        Map<Long, long[]> aggregated = new HashMap<>();
        for (Map.Entry<Long, List<ProductMetricsHourlyEntity>> entry : byProduct.entrySet()) {
            long totalViews = entry.getValue().stream()
                    .mapToLong(ProductMetricsHourlyEntity::getViewCount).sum();
            long totalLikes = entry.getValue().stream()
                    .mapToLong(ProductMetricsHourlyEntity::getLikeCount).sum();
            long totalOrders = entry.getValue().stream()
                    .mapToLong(ProductMetricsHourlyEntity::getOrderCount).sum();
            aggregated.put(entry.getKey(), new long[]{totalViews, totalLikes, totalOrders});
        }

        for (RankWeightVersion version : weightProperties.getVersions()) {
            Map<Long, Double> scores = new HashMap<>();
            aggregated.forEach((productId, counts) ->
                    scores.put(productId,
                            version.computeScore(counts[0], counts[1], counts[2])));
            rankRedisUpdater.replaceScores(version.versionKey(), date, scores);
        }

        log.info("[Ranking] rebuild 완료 — {} ({}개 상품, {}개 버전)",
                date, aggregated.size(), weightProperties.getVersions().size());
    }
}
