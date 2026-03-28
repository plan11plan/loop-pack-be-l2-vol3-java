package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponService;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssueBatchBuffer {

    private final JdbcTemplate jdbcTemplate;
    private final CouponService couponService;

    private final ConcurrentLinkedQueue<CouponIssueMessage> buffer
            = new ConcurrentLinkedQueue<>();

    public void add(Long couponId, Long userId) {
        buffer.add(new CouponIssueMessage(couponId, userId, System.currentTimeMillis()));
    }

    @Scheduled(fixedDelay = 3000)
    public void flush() {
        List<CouponIssueMessage> batch = new ArrayList<>();
        CouponIssueMessage msg;
        while ((msg = buffer.poll()) != null) {
            batch.add(msg);
        }
        if (batch.isEmpty()) return;

        Map<Long, CouponModel> couponCache = batch.stream()
                .map(CouponIssueMessage::couponId)
                .distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        couponService::getById));

        Timestamp now = Timestamp.from(ZonedDateTime.now().toInstant());

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO owned_coupons
                (coupon_id, user_id, coupon_name, discount_type,
                 discount_value, min_order_amount, expired_at,
                 created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i)
                            throws SQLException {
                        CouponIssueMessage m = batch.get(i);
                        CouponModel c = couponCache.get(m.couponId());
                        ps.setLong(1, m.couponId());
                        ps.setLong(2, m.userId());
                        ps.setString(3, c.getName());
                        ps.setString(4, c.getDiscountType().name());
                        ps.setLong(5, c.getDiscountValue());
                        ps.setObject(6, c.getMinOrderAmount());
                        ps.setObject(7, Timestamp.from(c.getExpiredAt().toInstant()));
                        ps.setTimestamp(8, now);
                        ps.setTimestamp(9, now);
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                });
    }

    public record CouponIssueMessage(Long couponId, Long userId, long issuedAt) {}
}
