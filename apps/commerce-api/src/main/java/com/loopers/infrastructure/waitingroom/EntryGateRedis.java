package com.loopers.infrastructure.waitingroom;

import com.loopers.config.redis.RedisConfig;
import com.loopers.domain.waitingroom.EntryGate;
import com.loopers.domain.waitingroom.WaitingRoomErrorCode;
import com.loopers.support.error.CoreException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EntryGateRedis implements EntryGate {

    private static final String TOKEN_KEY_PREFIX = "queue:entry-token:";
    private static final String ACTIVE_SET_KEY = "queue:entry-gate-members";

    private final RedisTemplate<String, String> redisTemplate;
    private final long tokenTtlSeconds;

    public EntryGateRedis(
            @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) RedisTemplate<String, String> redisTemplate,
            @Value("${queue.token.ttl-seconds:300}") long tokenTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    @Override
    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        String userIdStr = String.valueOf(userId);
        redisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + userId, token, tokenTtlSeconds, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(
                ACTIVE_SET_KEY, userIdStr, System.currentTimeMillis() + tokenTtlSeconds * 1000);
        return token;
    }

    @Override
    public String getToken(Long userId) {
        return redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
    }

    @Override
    public void validateToken(Long userId, String token) {
        String stored = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + userId);
        if (stored == null || !stored.equals(token)) {
            throw new CoreException(WaitingRoomErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public void completeEntry(Long userId) {
        redisTemplate.delete(TOKEN_KEY_PREFIX + userId);
        redisTemplate.opsForZSet().remove(ACTIVE_SET_KEY, String.valueOf(userId));
    }

    @Override
    public long getActiveCount() {
        // 만료된 항목 제거 후 카운트 (score = 만료시각, 현재시각 이전이면 만료)
        redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_SET_KEY, 0, System.currentTimeMillis());
        Long count = redisTemplate.opsForZSet().zCard(ACTIVE_SET_KEY);
        return count != null ? count : 0;
    }
}
