package com.loopers.batch.job.ranking.aggregate;

import java.time.LocalDate;

public record RankAggregateStagingRow(
        Long productId,
        long viewSum,
        long likeSum,
        long orderSum,
        double score,
        LocalDate maxMetricDate) {
}
