package com.loopers.domain.coupon;

import com.loopers.domain.coupon.dto.CouponCommand;
import com.loopers.support.error.CoreException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final OwnedCouponRepository ownedCouponRepository;

    public CouponModel register(CouponCommand.Create command) {
        return couponRepository.save(CouponModel.create(
                command.name(), command.discountType(), command.discountValue(),
                command.minOrderAmount(), command.totalQuantity(), command.expiredAt()));
    }

    public CouponModel getById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new CoreException(CouponErrorCode.NOT_FOUND));
    }

    public Page<CouponModel> getAll(Pageable pageable) {
        return couponRepository.findAll(pageable);
    }

    public void update(Long id, CouponCommand.Update command) {
        getById(id).update(command.name(), command.expiredAt());
    }

    public void delete(Long id) {
        getById(id).delete();
    }

    public Page<OwnedCouponModel> getIssuedCoupons(Long couponId, Pageable pageable) {
        return ownedCouponRepository.findAllByCouponId(couponId, pageable);
    }

    public OwnedCouponModel issue(Long couponId, Long userId) {
        CouponModel coupon = getById(couponId);
        ownedCouponRepository.findByCouponIdAndUserId(couponId, userId)
                .ifPresent(owned -> {
                    throw new CoreException(CouponErrorCode.ALREADY_ISSUED);
                });
        coupon.issue();
        return ownedCouponRepository.save(OwnedCouponModel.create(coupon, userId));
    }

    public long useAndCalculateDiscount(Long ownedCouponId, Long userId, long orderAmount) {
        OwnedCouponModel owned = ownedCouponRepository.findById(ownedCouponId)
                .orElseThrow(() -> new CoreException(CouponErrorCode.NOT_FOUND));
        CouponModel coupon = owned.getCoupon();
        coupon.validateMinOrderAmount(orderAmount);
        owned.use(userId);
        return coupon.calculateDiscount(orderAmount);
    }

    public void restoreOwnedCoupon(Long ownedCouponId) {
        ownedCouponRepository.findById(ownedCouponId)
                .orElseThrow(() -> new CoreException(CouponErrorCode.NOT_FOUND))
                .restore();
    }

    public List<OwnedCouponModel> getMyOwnedCoupons(Long userId) {
        return ownedCouponRepository.findAllByUserId(userId);
    }
}
