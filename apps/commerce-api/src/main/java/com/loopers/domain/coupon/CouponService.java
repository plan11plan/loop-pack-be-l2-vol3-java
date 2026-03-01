package com.loopers.domain.coupon;

import com.loopers.domain.coupon.dto.CouponCommand;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponService {

    public CouponModel register(CouponCommand.Create command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public CouponModel getById(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Page<CouponModel> getAll(Pageable pageable) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void update(Long id, CouponCommand.Update command) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public Page<OwnedCouponModel> getIssuedCoupons(Long couponId, Pageable pageable) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public OwnedCouponModel issue(Long couponId, Long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<OwnedCouponModel> getMyOwnedCoupons(Long userId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
