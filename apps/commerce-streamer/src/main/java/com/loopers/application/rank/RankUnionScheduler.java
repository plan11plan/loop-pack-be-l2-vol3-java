package com.loopers.application.rank;

import com.loopers.infrastructure.rank.RankRedisUpdater;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankUnionScheduler {

    private final RankRedisUpdater rankingRedisUpdater;

    @Scheduled(fixedRate = 5000)
    public void unionStoreRanking() {
        rankingRedisUpdater.unionStoreRanking(LocalDate.now());
    }
}
