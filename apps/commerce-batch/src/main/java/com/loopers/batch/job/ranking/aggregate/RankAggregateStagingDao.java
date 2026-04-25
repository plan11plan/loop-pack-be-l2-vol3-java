package com.loopers.batch.job.ranking.aggregate;

import jakarta.annotation.PostConstruct;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankAggregateStagingDao {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS rank_aggregate_staging (
                job_run_id      VARCHAR(80) NOT NULL,
                product_id      BIGINT      NOT NULL,
                view_sum        BIGINT      NOT NULL DEFAULT 0,
                like_sum        BIGINT      NOT NULL DEFAULT 0,
                order_sum       BIGINT      NOT NULL DEFAULT 0,
                score           DOUBLE      NOT NULL DEFAULT 0,
                max_metric_date DATE        NOT NULL,
                PRIMARY KEY (job_run_id, product_id),
                KEY idx_score (job_run_id, score, max_metric_date)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO rank_aggregate_staging
                (job_run_id, product_id, view_sum, like_sum, order_sum, score, max_metric_date)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                view_sum  = view_sum  + VALUES(view_sum),
                like_sum  = like_sum  + VALUES(like_sum),
                order_sum = order_sum + VALUES(order_sum),
                score     = score     + VALUES(score),
                max_metric_date = GREATEST(max_metric_date, VALUES(max_metric_date))
            """;

    private static final String SELECT_TOP_SQL = """
            SELECT product_id, view_sum, like_sum, order_sum, score, max_metric_date
            FROM rank_aggregate_staging
            WHERE job_run_id = ?
            ORDER BY score DESC, max_metric_date DESC
            LIMIT ?
            """;

    private static final String DELETE_SQL =
            "DELETE FROM rank_aggregate_staging WHERE job_run_id = ?";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureTable() {
        jdbcTemplate.execute(CREATE_TABLE_SQL);
    }

    public void upsertChunk(String jobRunId, List<RankAggregateStagingRow> rows) {
        if (rows.isEmpty()) return;

        jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RankAggregateStagingRow row = rows.get(i);
                ps.setString(1, jobRunId);
                ps.setLong(2, row.productId());
                ps.setLong(3, row.viewSum());
                ps.setLong(4, row.likeSum());
                ps.setLong(5, row.orderSum());
                ps.setDouble(6, row.score());
                ps.setDate(7, Date.valueOf(row.maxMetricDate()));
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });
    }

    public List<RankAggregateStagingRow> findTop(String jobRunId, int limit) {
        return jdbcTemplate.query(SELECT_TOP_SQL,
                (rs, rowNum) -> new RankAggregateStagingRow(
                        rs.getLong("product_id"),
                        rs.getLong("view_sum"),
                        rs.getLong("like_sum"),
                        rs.getLong("order_sum"),
                        rs.getDouble("score"),
                        rs.getDate("max_metric_date").toLocalDate()),
                jobRunId, limit);
    }

    public int deleteByJobRunId(String jobRunId) {
        return jdbcTemplate.update(DELETE_SQL, jobRunId);
    }
}
