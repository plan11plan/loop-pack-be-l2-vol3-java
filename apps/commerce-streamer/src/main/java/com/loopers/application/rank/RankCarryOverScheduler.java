package com.loopers.application.rank;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.rank.RankWeightProperties;
import com.loopers.domain.rank.RankWeightVersion;
import com.loopers.infrastructure.rank.RankRedisUpdater;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RankCarryOverScheduler {

    private static final double CARRY_OVER_WEIGHT = 0.1;
    private static final long TTL_SECONDS = 172800;

    private final RedisTemplate<String, String> redisTemplate;
    private final RankRedisUpdater rankRedisUpdater;
    private final RankWeightProperties weightProperties;

    public RankCarryOverScheduler(
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> redisTemplate,
            RankRedisUpdater rankRedisUpdater,
            RankWeightProperties weightProperties) {
        this.redisTemplate = redisTemplate;
        this.rankRedisUpdater = rankRedisUpdater;
        this.weightProperties = weightProperties;
    }

    @Scheduled(cron = "0 50 23 * * *")
    public void carryOver() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        for (RankWeightVersion version : weightProperties.getVersions()) {
            carryOverForVersion(version.versionKey(), today, tomorrow);
        }
    }

    private void carryOverForVersion(String versionKey, LocalDate today, LocalDate tomorrow) {
        String todayKey = rankRedisUpdater.allKeyOf(versionKey, today);
        String tomorrowKey = rankRedisUpdater.allKeyOf(versionKey, tomorrow);

        Set<TypedTuple<String>> todayScores = redisTemplate.opsForZSet()
                .rangeWithScores(todayKey, 0, -1);
        if (todayScores == null || todayScores.isEmpty()) {
            log.info("[Ranking] carry-over 스킵 — {} 데이터 없음", todayKey);
            return;
        }

        for (TypedTuple<String> tuple : todayScores) {
            double carriedScore = (tuple.getScore() != null ? tuple.getScore() : 0) * CARRY_OVER_WEIGHT;
            redisTemplate.opsForZSet()
                    .addIfAbsent(tomorrowKey, tuple.getValue(), carriedScore);
        }
        redisTemplate.expire(tomorrowKey, Duration.ofSeconds(TTL_SECONDS));

        Long tomorrowSize = redisTemplate.opsForZSet().zCard(tomorrowKey);
        log.info("[Ranking] carry-over 완료 — {} → {} ({}건, weight={})",
                todayKey, tomorrowKey, tomorrowSize, CARRY_OVER_WEIGHT);
    }
}
