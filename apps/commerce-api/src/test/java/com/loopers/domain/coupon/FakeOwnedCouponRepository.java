package com.loopers.domain.coupon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class FakeOwnedCouponRepository implements OwnedCouponRepository {

    private final Map<Long, OwnedCouponModel> store = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public OwnedCouponModel save(OwnedCouponModel ownedCouponModel) {
        if (ownedCouponModel.getId() == 0L) {
            setId(ownedCouponModel, idGenerator.getAndIncrement());
        }
        store.put(ownedCouponModel.getId(), ownedCouponModel);
        return ownedCouponModel;
    }

    @Override
    public Optional<OwnedCouponModel> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<OwnedCouponModel> findByCouponIdAndUserId(Long couponId, Long userId) {
        return store.values().stream()
                .filter(owned -> owned.getCoupon().getId().equals(couponId))
                .filter(owned -> owned.getUserId().equals(userId))
                .findFirst();
    }

    @Override
    public List<OwnedCouponModel> findAllByUserId(Long userId) {
        return store.values().stream()
                .filter(owned -> owned.getUserId().equals(userId))
                .toList();
    }

    @Override
    public Page<OwnedCouponModel> findAllByCouponId(Long couponId, Pageable pageable) {
        List<OwnedCouponModel> filtered = store.values().stream()
                .filter(owned -> owned.getCoupon().getId().equals(couponId))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());

        List<OwnedCouponModel> pageContent = start >= filtered.size()
                ? new ArrayList<>()
                : filtered.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    private void setId(Object target, Long id) {
        try {
            var field = target.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
