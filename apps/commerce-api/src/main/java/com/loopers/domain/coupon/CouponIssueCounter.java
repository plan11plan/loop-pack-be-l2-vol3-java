package com.loopers.domain.coupon;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public class CouponIssueCounter {

    private final ConcurrentHashMap<Long, CounterEntry> counters = new ConcurrentHashMap<>();

    public AcquireResult tryAcquire(Long couponId, Long userId,
                                    IntSupplier totalQuantitySupplier) {
        CounterEntry entry = counters.computeIfAbsent(
                couponId, k -> new CounterEntry(totalQuantitySupplier.getAsInt()));
        if (!entry.issuedUsers().add(userId)) {
            return AcquireResult.ALREADY_ISSUED;
        }
        if (entry.counter().incrementAndGet() <= entry.totalQuantity()) {
            return AcquireResult.SUCCESS;
        }
        // 수량 초과 — 카운터만 롤백, set은 유지 (재시도 방지)
        entry.counter().decrementAndGet();
        return AcquireResult.QUANTITY_EXHAUSTED;
    }

    public void release(Long couponId, Long userId) {
        CounterEntry entry = counters.get(couponId);
        if (entry != null) {
            entry.issuedUsers().remove(userId);
            entry.counter().decrementAndGet();
        }
    }

    public enum AcquireResult {
        SUCCESS,
        ALREADY_ISSUED,
        QUANTITY_EXHAUSTED
    }

    private record CounterEntry(
            int totalQuantity,
            AtomicInteger counter,
            Set<Long> issuedUsers) {

        CounterEntry(int totalQuantity) {
            this(totalQuantity, new AtomicInteger(0),
                    ConcurrentHashMap.newKeySet());
        }
    }
}
