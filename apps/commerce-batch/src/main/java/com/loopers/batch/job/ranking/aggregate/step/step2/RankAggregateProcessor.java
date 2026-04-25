package com.loopers.batch.job.ranking.aggregate.step.step2;

import com.loopers.batch.job.ranking.aggregate.RankAggregateStagingRow;
import com.loopers.domain.metrics.ProductMetricsDailyEntity;
import com.loopers.domain.rank.RankingScorePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

@RequiredArgsConstructor
public class RankAggregateProcessor
        implements ItemProcessor<ProductMetricsDailyEntity, RankAggregateStagingRow> {

    private final RankingScorePolicy scorePolicy;

    @Override
    public RankAggregateStagingRow process(ProductMetricsDailyEntity entity) {
        return new RankAggregateStagingRow(
                entity.getProductId(),
                entity.getViewCount(),
                entity.getLikeCount(),
                entity.getOrderCount(),
                scorePolicy.score(entity.getViewCount(), entity.getLikeCount(), entity.getOrderCount()),
                entity.getMetricDate());
    }
}
