package com.loopers.domain.coupon;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CouponIssueCounter {

    private final ConcurrentHashMap<Long, AtomicInteger> counters = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long couponId, int totalQuantity) {
        AtomicInteger counter = counters.computeIfAbsent(couponId, k -> new AtomicInteger(0));
        if (counter.incrementAndGet() <= totalQuantity) {
            return true;
        }
        counter.decrementAndGet();
        return false;
    }

    public void release(Long couponId) {
        AtomicInteger counter = counters.get(couponId);
        if (counter != null) {
            counter.decrementAndGet();
        }
    }
}
