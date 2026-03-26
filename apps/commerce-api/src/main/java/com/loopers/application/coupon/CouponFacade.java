package com.loopers.application.coupon;

import com.loopers.application.coupon.dto.CouponCriteria;
import com.loopers.application.coupon.dto.CouponResult;
import com.loopers.domain.coupon.CouponErrorCode;
import com.loopers.domain.coupon.CouponIssueCounter;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponService;
import com.loopers.support.error.CoreException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponService couponService;
    private final CouponIssueCounter couponIssueCounter = new CouponIssueCounter();

    @Transactional
    public CouponResult.Detail registerCoupon(CouponCriteria.Create criteria) {
        return CouponResult.Detail.from(
                couponService.register(criteria.toCommand()));
    }

    @Transactional(readOnly = true)
    public CouponResult.Detail getCoupon(Long couponId) {
        return CouponResult.Detail.from(
                couponService.getById(couponId));
    }

    @Transactional(readOnly = true)
    public Page<CouponResult.Detail> getCoupons(Pageable pageable) {
        return couponService.getAll(pageable)
                .map(CouponResult.Detail::from);
    }

    @Transactional
    public void updateCoupon(Long couponId, CouponCriteria.Update criteria) {
        couponService.update(couponId, criteria.toCommand());
    }

    @Transactional
    public void deleteCoupon(Long couponId) {
        couponService.delete(couponId);
    }

    @Transactional(readOnly = true)
    public Page<CouponResult.IssuedDetail> getIssuedCoupons(Long couponId, Pageable pageable) {
        return couponService.getIssuedCoupons(couponId, pageable)
                .map(CouponResult.IssuedDetail::from);
    }

    public CouponResult.IssuedDetail issueCoupon(Long couponId, Long userId) {
        // 1차 문지기: AtomicInteger (트랜잭션 밖, DB 부하 경감)
        CouponModel coupon = couponService.getById(couponId);
        if (!couponIssueCounter.tryAcquire(couponId, coupon.getTotalQuantity())) {
            throw new CoreException(CouponErrorCode.QUANTITY_EXHAUSTED);
        }
        try {
            // 2차 문지기 + OwnedCoupon INSERT (CouponService @Transactional)
            return CouponResult.IssuedDetail.from(
                    couponService.issue(couponId, userId));
        } catch (Exception e) {
            couponIssueCounter.release(couponId);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<CouponResult.OwnedDetail> getMyOwnedCoupons(Long userId) {
        return couponService.getMyOwnedCoupons(userId).stream()
                .map(CouponResult.OwnedDetail::from)
                .toList();
    }
}
