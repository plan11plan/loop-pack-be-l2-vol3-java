package com.loopers.batch.job.ranking.aggregate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.CommerceBatchApplication;
import com.loopers.domain.metrics.ProductMetricsDailyEntity;
import com.loopers.domain.rank.ProductRankWeeklyEntity;
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository;
import com.loopers.infrastructure.rank.ProductRankWeeklyJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = CommerceBatchApplication.class)
@SpringBatchTest
@TestPropertySource(properties = "spring.batch.job.name=" + WeeklyRankJobConfig.JOB_NAME)
class WeeklyRankJobE2ETest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(WeeklyRankJobConfig.JOB_NAME)
    private Job job;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private ProductMetricsDailyJpaRepository dailyRepo;

    @Autowired
    private ProductRankWeeklyJpaRepository weeklyRepo;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 4, 15);
    private static final java.util.concurrent.atomic.AtomicLong RUN_COUNTER =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

    @BeforeEach
    void setUp() {
        databaseCleanUp.truncateAllTables();
        jobLauncherTestUtils.setJob(job);
    }

    @DisplayName("targetDate 파라미터 없이 실행하면 Job이 실패한다.")
    @Test
    void shouldFail_whenNoTargetDate() throws Exception {
        var jobExecution = jobLauncherTestUtils.launchJob();

        assertThat(jobExecution.getExitStatus().getExitCode())
                .isEqualTo(ExitStatus.FAILED.getExitCode());
    }

    @DisplayName("정상 실행 시 score 내림차순으로 TOP 랭킹이 저장된다.")
    @Test
    void success() throws Exception {
        insertBaseData();

        var jobExecution = jobLauncherTestUtils.launchJob(buildParams());

        assertAll(
                () -> assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED),
                () -> assertThat(jobExecution.getExitStatus().getExitCode())
                        .isEqualTo(ExitStatus.COMPLETED.getExitCode()));

        List<ProductRankWeeklyEntity> results =
                weeklyRepo.findByYearMonthWeekOrderByRankValueAsc(TARGET_DATE.toString());

        assertThat(results).hasSize(3);
        assertThat(results).extracting(ProductRankWeeklyEntity::getRankValue)
                .containsExactly(1, 2, 3);
        assertThat(results).extracting(ProductRankWeeklyEntity::getProductId)
                .containsExactly(3L, 2L, 1L);
    }

    @DisplayName("동점자가 있을 때 max_metric_date가 최신인 product가 더 높은 순위를 받는다.")
    @Test
    void tieBreakBy_maxMetricDate() throws Exception {
        LocalDate windowStart = TARGET_DATE.minusDays(7);

        // product 10: order=1 / 2026-04-09 (오래된 날짜)
        saveDaily(10L, windowStart, 0, 0, 1);
        // product 20: order=1 / 2026-04-14 (최신 날짜) — 동점이지만 위쪽
        saveDaily(20L, windowStart.plusDays(5), 0, 0, 1);

        jobLauncherTestUtils.launchJob(buildParams());

        List<ProductRankWeeklyEntity> results =
                weeklyRepo.findByYearMonthWeekOrderByRankValueAsc(TARGET_DATE.toString());

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getProductId()).isEqualTo(20L);
        assertThat(results.get(1).getProductId()).isEqualTo(10L);
    }

    @DisplayName("재실행 시 직전 회차에 있던 stale row를 정리한다.")
    @Test
    void cleansStaleRow_onRerun() throws Exception {
        insertBaseData();
        jobLauncherTestUtils.launchJob(buildParams());

        databaseCleanUp.truncateAllTables();
        // 1회차 결과 시뮬레이션: product 999는 직전 결과에만 존재
        weeklyRepo.save(ProductRankWeeklyEntity.of(
                50, 999L, 1.0, 0, 0, 0, TARGET_DATE.toString()));
        insertBaseData();

        jobLauncherTestUtils.launchJob(buildParams());

        List<ProductRankWeeklyEntity> results =
                weeklyRepo.findByYearMonthWeekOrderByRankValueAsc(TARGET_DATE.toString());
        assertThat(results).hasSize(3);
        assertThat(results).extracting(ProductRankWeeklyEntity::getProductId)
                .doesNotContain(999L);
    }

    @DisplayName("staging 테이블은 cleanupStep 후 비워진다.")
    @Test
    void stagingCleared_afterJob() throws Exception {
        insertBaseData();

        jobLauncherTestUtils.launchJob(buildParams());

        Long stagingCount = stagingCount();
        assertThat(stagingCount).isZero();
    }

    private Long stagingCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rank_aggregate_staging WHERE job_run_id = ?",
                Long.class,
                "weekly:" + TARGET_DATE);
    }

    private org.springframework.batch.core.JobParameters buildParams() {
        return new JobParametersBuilder()
                .addString("targetDate", TARGET_DATE.toString())
                .addLong("run.id", RUN_COUNTER.incrementAndGet())
                .toJobParameters();
    }

    private void insertBaseData() {
        LocalDate windowStart = TARGET_DATE.minusDays(7);
        // product 1: 조회 50 → score = 50
        saveDaily(1L, windowStart, 30, 0, 0);
        saveDaily(1L, windowStart.plusDays(1), 20, 0, 0);
        // product 2: 좋아요 20 → score = 60
        saveDaily(2L, windowStart, 0, 15, 0);
        saveDaily(2L, windowStart.plusDays(2), 0, 5, 0);
        // product 3: 주문 10 → score = 100
        saveDaily(3L, windowStart, 0, 0, 6);
        saveDaily(3L, windowStart.plusDays(3), 0, 0, 4);
    }

    private void saveDaily(Long productId, LocalDate date, long views, long likes, long orders) {
        dailyRepo.save(buildDailyEntity(productId, date, views, likes, orders));
    }

    private ProductMetricsDailyEntity buildDailyEntity(Long productId, LocalDate date,
            long views, long likes, long orders) {
        try {
            var constructor = ProductMetricsDailyEntity.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            var entity = constructor.newInstance();

            setField(entity, "productId", productId);
            setField(entity, "metricDate", date);
            setField(entity, "viewCount", views);
            setField(entity, "likeCount", likes);
            setField(entity, "orderCount", orders);
            setField(entity, "updatedAt", ZonedDateTime.now());
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        var field = ProductMetricsDailyEntity.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
