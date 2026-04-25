package com.loopers.batch.job.ranking.aggregate;

import com.loopers.batch.job.ranking.aggregate.step.step1.ClearStepTasklet;
import com.loopers.batch.job.ranking.aggregate.step.step2.RankAggregateProcessor;
import com.loopers.batch.job.ranking.aggregate.step.step2.StagingUpsertWriter;
import com.loopers.batch.job.ranking.aggregate.step.step3.RankStepTasklet;
import com.loopers.batch.job.ranking.aggregate.step.step4.CleanupStepTasklet;
import com.loopers.batch.job.ranking.listener.JobListener;
import com.loopers.batch.job.ranking.listener.StepMonitorListener;
import com.loopers.batch.job.ranking.reader.QueryDslZeroOffsetItemReader;
import com.loopers.domain.metrics.ProductMetricsDailyEntity;
import com.loopers.domain.rank.RankingScorePolicy;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@ConditionalOnExpression(
        "'${spring.batch.job.name:NONE}' == '" + MonthlyRankJobConfig.JOB_NAME + "'"
                + " or '${spring.main.web-application-type:servlet}' != 'none'")
@RequiredArgsConstructor
@Configuration
public class MonthlyRankJobConfig {

    public static final String JOB_NAME = "monthlyRankJob";
    private static final String CLEAR_STEP_NAME = "clearMonthlyRankStep";
    private static final String ACCUMULATE_STEP_NAME = "accumulateMonthlyRankStep";
    private static final String RANK_STEP_NAME = "rankMonthlyRankStep";
    private static final String CLEANUP_STEP_NAME = "cleanupMonthlyRankStep";

    private static final String TARGET_TABLE = "mv_product_rank_monthly";
    private static final String PERIOD_COLUMN = "period_month";
    private static final String JOB_RUN_ID_PREFIX = "monthly:";
    private static final int WINDOW_DAYS = 30;
    private static final int CHUNK_SIZE = 2_000;
    private static final int TOP_N = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    private final JobListener jobListener;
    private final StepMonitorListener stepMonitorListener;
    private final RankingScorePolicy rankingScorePolicy;
    private final RankAggregateStagingDao stagingDao;

    @Bean(JOB_NAME)
    public Job monthlyRankJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(clearMonthlyRankStep(null))
                .next(accumulateMonthlyRankStep(null, null, null))
                .next(rankMonthlyRankStep(null))
                .next(cleanupMonthlyRankStep(null))
                .listener(jobListener)
                .build();
    }

    @JobScope
    @Bean(CLEAR_STEP_NAME)
    public Step clearMonthlyRankStep(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return new StepBuilder(CLEAR_STEP_NAME, jobRepository)
                .tasklet(
                        new ClearStepTasklet(jobRunId(targetDateStr), stagingDao),
                        transactionManager)
                .listener(stepMonitorListener)
                .build();
    }

    @JobScope
    @Bean(ACCUMULATE_STEP_NAME)
    public Step accumulateMonthlyRankStep(
            QueryDslZeroOffsetItemReader monthlyReader,
            ItemProcessor<ProductMetricsDailyEntity, RankAggregateStagingRow> monthlyProcessor,
            ItemWriter<RankAggregateStagingRow> monthlyStagingWriter) {
        return new StepBuilder(ACCUMULATE_STEP_NAME, jobRepository)
                .<ProductMetricsDailyEntity, RankAggregateStagingRow>chunk(
                        CHUNK_SIZE, transactionManager)
                .reader(monthlyReader)
                .processor(monthlyProcessor)
                .writer(monthlyStagingWriter)
                .listener(stepMonitorListener)
                .build();
    }

    @JobScope
    @Bean(RANK_STEP_NAME)
    public Step rankMonthlyRankStep(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return new StepBuilder(RANK_STEP_NAME, jobRepository)
                .tasklet(
                        new RankStepTasklet(
                                jobRunId(targetDateStr),
                                targetDateStr,
                                TOP_N,
                                TARGET_TABLE,
                                PERIOD_COLUMN,
                                stagingDao,
                                jdbcTemplate),
                        transactionManager)
                .listener(stepMonitorListener)
                .build();
    }

    @JobScope
    @Bean(CLEANUP_STEP_NAME)
    public Step cleanupMonthlyRankStep(@Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return new StepBuilder(CLEANUP_STEP_NAME, jobRepository)
                .tasklet(
                        new CleanupStepTasklet(jobRunId(targetDateStr), stagingDao),
                        transactionManager)
                .listener(stepMonitorListener)
                .build();
    }

    @StepScope
    @Bean
    public QueryDslZeroOffsetItemReader monthlyReader(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        LocalDate endDate = LocalDate.parse(targetDateStr).minusDays(1L);
        LocalDate windowStart = endDate.minusDays(WINDOW_DAYS - 1L);
        return new QueryDslZeroOffsetItemReader(
                queryFactory, entityManager, CHUNK_SIZE, windowStart, endDate);
    }

    @StepScope
    @Bean
    public ItemProcessor<ProductMetricsDailyEntity, RankAggregateStagingRow> monthlyProcessor() {
        return new RankAggregateProcessor(rankingScorePolicy);
    }

    @StepScope
    @Bean
    public ItemWriter<RankAggregateStagingRow> monthlyStagingWriter(
            @Value("#{jobParameters['targetDate']}") String targetDateStr) {
        return new StagingUpsertWriter(jobRunId(targetDateStr), stagingDao);
    }

    private static String jobRunId(String periodKey) {
        return JOB_RUN_ID_PREFIX + periodKey;
    }
}
