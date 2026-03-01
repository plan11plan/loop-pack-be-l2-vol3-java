package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.OwnedCouponModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedCouponJpaRepository extends JpaRepository<OwnedCouponModel, Long> {

    Optional<OwnedCouponModel> findByCouponIdAndUserId(Long couponId, Long userId);

    List<OwnedCouponModel> findAllByUserId(Long userId);

    Page<OwnedCouponModel> findAllByCouponId(Long couponId, Pageable pageable);

    Optional<OwnedCouponModel> findByOrderId(Long orderId);
}
