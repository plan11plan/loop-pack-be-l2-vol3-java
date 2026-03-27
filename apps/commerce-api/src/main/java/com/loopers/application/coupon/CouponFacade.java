package com.loopers.application.coupon;

import com.loopers.application.coupon.dto.CouponCriteria;
import com.loopers.application.coupon.dto.CouponResult;
import com.loopers.domain.coupon.CouponErrorCode;
import com.loopers.domain.coupon.CouponIssueCounter;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponService;
import com.loopers.support.error.CoreException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
                couponService.register(criteria.toCommand()), 0);
    }

    @Transactional(readOnly = true)
    public CouponResult.Detail getCoupon(Long couponId) {
        return CouponResult.Detail.from(
                couponService.getById(couponId),
                couponService.countIssuedCoupons(couponId));
    }

    @Transactional(readOnly = true)
    public Page<CouponResult.Detail> getCoupons(Pageable pageable) {
        Page<CouponModel> coupons = couponService.getAll(pageable);
        List<Long> couponIds = coupons.getContent().stream()
                .map(CouponModel::getId)
                .toList();
        Map<Long, Long> issuedCountMap = couponService.countIssuedCoupons(couponIds);
        return coupons.map(coupon -> CouponResult.Detail.from(
                coupon, issuedCountMap.getOrDefault(coupon.getId(), 0L)));
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
        // 1차 문지기: AtomicInteger + 중복 유저 필터 — 첫 요청만 DB 조회, 이후 캐시
        CouponIssueCounter.AcquireResult acquireResult =
                couponIssueCounter.tryAcquire(couponId, userId,
                        () -> couponService.getById(couponId).getTotalQuantity());
        if (acquireResult != CouponIssueCounter.AcquireResult.SUCCESS) {
            throw new CoreException(
                    acquireResult == CouponIssueCounter.AcquireResult.ALREADY_ISSUED
                            ? CouponErrorCode.ALREADY_ISSUED
                            : CouponErrorCode.QUANTITY_EXHAUSTED);
        }
        try {
            return CouponResult.IssuedDetail.from(
                    couponService.issue(couponId, userId));
        } catch (DataIntegrityViolationException e) {
            couponIssueCounter.release(couponId, userId);
            throw new CoreException(CouponErrorCode.ALREADY_ISSUED);
        }
    }

    @Transactional(readOnly = true)
    public List<CouponResult.OwnedDetail> getMyOwnedCoupons(Long userId) {
        return couponService.getMyOwnedCoupons(userId).stream()
                .map(CouponResult.OwnedDetail::from)
                .toList();
    }
}
