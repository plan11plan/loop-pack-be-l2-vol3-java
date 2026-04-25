package com.loopers.batch.job.ranking.aggregate.step.step3;

import com.loopers.batch.job.ranking.aggregate.RankAggregateStagingDao;
import com.loopers.batch.job.ranking.aggregate.RankAggregateStagingRow;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
public class RankStepTasklet implements Tasklet {

    private static final String UPSERT_TEMPLATE = """
            INSERT INTO %s
                (product_id, rank_value, score, view_count, like_count, order_count,
                 %s, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                rank_value  = VALUES(rank_value),
                score       = VALUES(score),
                view_count  = VALUES(view_count),
                like_count  = VALUES(like_count),
                order_count = VALUES(order_count),
                updated_at  = VALUES(updated_at)
            """;

    private final String jobRunId;
    private final String periodKey;
    private final int topN;
    private final String targetTable;
    private final RankAggregateStagingDao stagingDao;
    private final JdbcTemplate jdbcTemplate;
    private final String upsertSql;
    private final String deleteByPeriodSql;
    private final String deleteStaleSqlPrefix;

    public RankStepTasklet(
            String jobRunId,
            String periodKey,
            int topN,
            String targetTable,
            String periodColumn,
            RankAggregateStagingDao stagingDao,
            JdbcTemplate jdbcTemplate) {
        this.jobRunId = jobRunId;
        this.periodKey = periodKey;
        this.topN = topN;
        this.targetTable = targetTable;
        this.stagingDao = stagingDao;
        this.jdbcTemplate = jdbcTemplate;
        this.upsertSql = UPSERT_TEMPLATE.formatted(targetTable, periodColumn);
        this.deleteByPeriodSql =
                "DELETE FROM %s WHERE %s = ?".formatted(targetTable, periodColumn);
        this.deleteStaleSqlPrefix =
                "DELETE FROM %s WHERE %s = ? AND product_id NOT IN ("
                        .formatted(targetTable, periodColumn);
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        List<RankAggregateStagingRow> top = stagingDao.findTop(jobRunId, topN);
        if (top.isEmpty()) {
            int deletedAll = jdbcTemplate.update(deleteByPeriodSql, periodKey);
            log.info("[Rank] no candidates. table={} deletedAll={}", targetTable, deletedAll);
            return RepeatStatus.FINISHED;
        }
        upsertTopN(top);
        int deletedStale = deleteStaleRows(top);
        log.info("[Rank] table={} upsert={} deletedStale={}",
                targetTable, top.size(), deletedStale);
        return RepeatStatus.FINISHED;
    }

    private void upsertTopN(List<RankAggregateStagingRow> top) {
        Timestamp now = Timestamp.from(ZonedDateTime.now().toInstant());
        jdbcTemplate.batchUpdate(upsertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RankAggregateStagingRow row = top.get(i);
                ps.setLong(1, row.productId());
                ps.setInt(2, i + 1);
                ps.setDouble(3, row.score());
                ps.setLong(4, row.viewSum());
                ps.setLong(5, row.likeSum());
                ps.setLong(6, row.orderSum());
                ps.setString(7, periodKey);
                ps.setTimestamp(8, now);
            }

            @Override
            public int getBatchSize() {
                return top.size();
            }
        });
    }

    private int deleteStaleRows(List<RankAggregateStagingRow> top) {
        StringBuilder sql = new StringBuilder(deleteStaleSqlPrefix);
        for (int i = 0; i < top.size(); i++) {
            sql.append(i == 0 ? "?" : ",?");
        }
        sql.append(")");

        Object[] args = new Object[top.size() + 1];
        args[0] = periodKey;
        for (int i = 0; i < top.size(); i++) {
            args[i + 1] = top.get(i).productId();
        }
        return jdbcTemplate.update(sql.toString(), args);
    }
}
