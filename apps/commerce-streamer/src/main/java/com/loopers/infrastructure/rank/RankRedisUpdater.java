package com.loopers.infrastructure.rank;

import com.loopers.config.redis.RedisConfig;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RankRedisUpdater {

    private static final String ALL_PREFIX = "ranking:all:";
    private static final Duration TTL = Duration.ofDays(2);

    private final RedisTemplate<String, String> redisTemplate;

    public RankRedisUpdater(
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void replaceScores(String version, LocalDate date, Map<Long, Double> scores) {
        String key = allKeyOf(version, date);
        redisTemplate.delete(key);
        scores.forEach((productId, score) ->
                redisTemplate.opsForZSet()
                        .add(key, String.valueOf(productId), score));
        redisTemplate.expire(key, TTL);
        log.info("[Ranking] REPLACE 완료 — {} ({}건)", key, scores.size());
    }

    public String allKeyOf(String version, LocalDate date) {
        return ALL_PREFIX + version + ":" + date.format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}
