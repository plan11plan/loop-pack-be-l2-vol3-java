package com.loopers.batch.job.ranking.aggregate.step.step2;

import com.loopers.batch.job.ranking.aggregate.RankAggregateStagingDao;
import com.loopers.batch.job.ranking.aggregate.RankAggregateStagingRow;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
public class StagingUpsertWriter implements ItemWriter<RankAggregateStagingRow> {

    private final String jobRunId;
    private final RankAggregateStagingDao dao;

    @Override
    public void write(Chunk<? extends RankAggregateStagingRow> chunk) {
        Map<Long, MutableAggregate> folded = new HashMap<>();
        for (RankAggregateStagingRow row : chunk) {
            folded.computeIfAbsent(row.productId(), id -> new MutableAggregate())
                    .add(row);
        }
        List<RankAggregateStagingRow> rows = new ArrayList<>(folded.size());
        folded.forEach((productId, agg) -> rows.add(agg.toRow(productId)));
        dao.upsertChunk(jobRunId, rows);
    }

    private static final class MutableAggregate {
        private long viewSum;
        private long likeSum;
        private long orderSum;
        private double score;
        private LocalDate maxMetricDate;

        void add(RankAggregateStagingRow row) {
            this.viewSum += row.viewSum();
            this.likeSum += row.likeSum();
            this.orderSum += row.orderSum();
            this.score += row.score();
            this.maxMetricDate = (maxMetricDate == null || row.maxMetricDate().isAfter(maxMetricDate))
                    ? row.maxMetricDate()
                    : maxMetricDate;
        }

        RankAggregateStagingRow toRow(Long productId) {
            return new RankAggregateStagingRow(
                    productId, viewSum, likeSum, orderSum, score, maxMetricDate);
        }
    }
}
