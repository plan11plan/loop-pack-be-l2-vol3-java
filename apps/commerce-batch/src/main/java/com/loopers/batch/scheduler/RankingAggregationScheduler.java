package com.loopers.batch.scheduler;

import com.loopers.batch.job.ranking.aggregate.MonthlyRankJobConfig;
import com.loopers.batch.job.ranking.aggregate.WeeklyRankJobConfig;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "ranking.scheduler.enabled", havingValue = "true")
public class RankingAggregationScheduler {

    private static final String ZONE = "Asia/Seoul";
    private static final String WEEKLY_CRON = "0 5 0 * * *";
    private static final String MONTHLY_CRON = "0 30 0 * * *";

    private final JobLauncher jobLauncher;
    private final Job weeklyRankJob;
    private final Job monthlyRankJob;

    @Scheduled(cron = WEEKLY_CRON, zone = ZONE)
    public void runWeekly() {
        launch(weeklyRankJob, WeeklyRankJobConfig.JOB_NAME);
    }

    @Scheduled(cron = MONTHLY_CRON, zone = ZONE)
    public void runMonthly() {
        launch(monthlyRankJob, MonthlyRankJobConfig.JOB_NAME);
    }

    private void launch(Job job, String jobName) {
        try {
            JobExecution execution = jobLauncher.run(job, new JobParametersBuilder()
                    .addString("targetDate", LocalDate.now(ZoneId.of(ZONE)).toString())
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters());
            log.info("[Scheduler] {} launched. status={} exitStatus={}",
                    jobName, execution.getStatus(), execution.getExitStatus().getExitCode());
        } catch (Exception e) {
            log.error("[Scheduler] {} failed to launch", jobName, e);
        }
    }
}
