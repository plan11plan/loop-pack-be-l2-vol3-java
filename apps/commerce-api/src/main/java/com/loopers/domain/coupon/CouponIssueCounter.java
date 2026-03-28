package com.loopers.domain.coupon;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;

public class CouponIssueCounter {

    private final ConcurrentHashMap<Long, CounterEntry> counters = new ConcurrentHashMap<>();

    public AcquireResult tryAcquire(Long couponId, Long userId, IntSupplier totalQuantitySupplier) {
        AtomicReference<AcquireResult> result = new AtomicReference<>();

        counters.compute(couponId, (key, entry) -> {
            if (entry == null) entry = new CounterEntry(totalQuantitySupplier.getAsInt());

            if (entry.issuedUsers().contains(userId)) {
                result.set(AcquireResult.ALREADY_ISSUED);
                return entry;
            }
            if (entry.counter().get() >= entry.totalQuantity()) {
                result.set(AcquireResult.QUANTITY_EXHAUSTED);
                return entry;
            }

            entry.issuedUsers().add(userId);
            entry.counter().incrementAndGet();
            result.set(AcquireResult.SUCCESS);
            return entry;
        });

        return result.get();
    }

    public void release(Long couponId, Long userId) {
        counters.compute(couponId, (key, entry) -> {
            if (entry == null) return null;
            entry.issuedUsers().remove(userId);
            entry.counter().decrementAndGet();
            return entry;
        });
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
