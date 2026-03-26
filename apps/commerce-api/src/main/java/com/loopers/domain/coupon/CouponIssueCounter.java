package com.loopers.domain.coupon;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public class CouponIssueCounter {

    private final ConcurrentHashMap<Long, CounterEntry> counters = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long couponId, IntSupplier totalQuantitySupplier) {
        CounterEntry entry = counters.computeIfAbsent(
                couponId, k -> new CounterEntry(totalQuantitySupplier.getAsInt()));
        if (entry.counter().incrementAndGet() <= entry.totalQuantity()) {
            return true;
        }
        entry.counter().decrementAndGet();
        return false;
    }

    public void release(Long couponId) {
        CounterEntry entry = counters.get(couponId);
        if (entry != null) {
            entry.counter().decrementAndGet();
        }
    }

    private record CounterEntry(int totalQuantity, AtomicInteger counter) {
        CounterEntry(int totalQuantity) {
            this(totalQuantity, new AtomicInteger(0));
        }
    }
}
