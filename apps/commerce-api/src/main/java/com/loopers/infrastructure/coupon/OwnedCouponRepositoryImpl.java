package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.OwnedCouponModel;
import com.loopers.domain.coupon.OwnedCouponRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OwnedCouponRepositoryImpl implements OwnedCouponRepository {

    private final OwnedCouponJpaRepository ownedCouponJpaRepository;

    @Override
    public OwnedCouponModel save(OwnedCouponModel ownedCouponModel) {
        return ownedCouponJpaRepository.save(ownedCouponModel);
    }

    @Override
    public Optional<OwnedCouponModel> findById(Long id) {
        return ownedCouponJpaRepository.findById(id);
    }

    @Override
    public Optional<OwnedCouponModel> findByCouponIdAndUserId(Long couponId, Long userId) {
        return ownedCouponJpaRepository.findByCouponIdAndUserId(couponId, userId);
    }

    @Override
    public List<OwnedCouponModel> findAllByUserId(Long userId) {
        return ownedCouponJpaRepository.findAllByUserId(userId);
    }

    @Override
    public Page<OwnedCouponModel> findAllByCouponId(Long couponId, Pageable pageable) {
        return ownedCouponJpaRepository.findAllByCouponId(couponId, pageable);
    }
}
