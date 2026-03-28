package com.loopers.application.coupon;

import com.loopers.application.coupon.dto.CouponCriteria;
import com.loopers.application.coupon.dto.CouponResult;
import com.loopers.domain.coupon.CouponErrorCode;
import com.loopers.domain.coupon.CouponIssueCounter;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.CouponService;
import com.loopers.infrastructure.coupon.CouponIssueBatchBuffer;
import com.loopers.support.error.CoreException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponService couponService;
    private final CouponIssueBatchBuffer couponIssueBatchBuffer;
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
        // 1차 판별기: 인메모리 원자적 판정 — DB 접근 0
        CouponIssueCounter.AcquireResult acquireResult =
                couponIssueCounter.tryAcquire(couponId, userId,
                        () -> couponService.getById(couponId).getTotalQuantity());

        if (acquireResult == CouponIssueCounter.AcquireResult.ALREADY_ISSUED) {
            throw new CoreException(CouponErrorCode.ALREADY_ISSUED);
        }
        if (acquireResult == CouponIssueCounter.AcquireResult.QUANTITY_EXHAUSTED) {
            throw new CoreException(CouponErrorCode.QUANTITY_EXHAUSTED);
        }

        // 버퍼에 추가하고 즉시 반환 — DB 접근 0
        couponIssueBatchBuffer.add(couponId, userId);
        return CouponResult.IssuedDetail.pending(couponId, userId);
    }

    @Transactional(readOnly = true)
    public List<CouponResult.OwnedDetail> getMyOwnedCoupons(Long userId) {
        return couponService.getMyOwnedCoupons(userId).stream()
                .map(CouponResult.OwnedDetail::from)
                .toList();
    }
}
