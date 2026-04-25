package com.loopers.batch.job.ranking.aggregate.step.step4;

import com.loopers.batch.job.ranking.aggregate.RankAggregateStagingDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Slf4j
@RequiredArgsConstructor
public class CleanupStepTasklet implements Tasklet {

    private final String jobRunId;
    private final RankAggregateStagingDao stagingDao;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int deleted = stagingDao.deleteByJobRunId(jobRunId);
        log.info("[Cleanup] jobRunId={} deleted={}", jobRunId, deleted);
        return RepeatStatus.FINISHED;
    }
}
