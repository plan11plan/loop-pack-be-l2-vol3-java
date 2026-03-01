package com.loopers.application.coupon.dto;

import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.OwnedCouponModel;
import java.time.ZonedDateTime;

public class CouponResult {

    public record Detail(
        Long id,
        String name,
        String discountType,
        long discountValue,
        Long minOrderAmount,
        int totalQuantity,
        int issuedQuantity,
        ZonedDateTime expiredAt,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt
    ) {
        public static Detail from(CouponModel model) {
            return new Detail(
                    model.getId(),
                    model.getName(),
                    model.getDiscountType().name(),
                    model.getDiscountValue(),
                    model.getMinOrderAmount(),
                    model.getTotalQuantity(),
                    model.getIssuedQuantity(),
                    model.getExpiredAt(),
                    model.getCreatedAt(),
                    model.getUpdatedAt());
        }
    }

    public record IssuedDetail(
        Long id,
        Long userId,
        String status,
        ZonedDateTime usedAt,
        ZonedDateTime createdAt
    ) {
        public static IssuedDetail from(OwnedCouponModel model) {
            return new IssuedDetail(
                    model.getId(),
                    model.getUserId(),
                    model.getStatus().name(),
                    model.getUsedAt(),
                    model.getCreatedAt());
        }
    }

    public record OwnedDetail(
        Long id,
        Long couponId,
        String couponName,
        String discountType,
        long discountValue,
        Long minOrderAmount,
        String status,
        ZonedDateTime expiredAt,
        ZonedDateTime usedAt,
        ZonedDateTime issuedAt
    ) {
        public static OwnedDetail from(OwnedCouponModel model) {
            CouponModel coupon = model.getCoupon();
            return new OwnedDetail(
                    model.getId(),
                    coupon.getId(),
                    coupon.getName(),
                    coupon.getDiscountType().name(),
                    coupon.getDiscountValue(),
                    coupon.getMinOrderAmount(),
                    model.getStatus().name(),
                    coupon.getExpiredAt(),
                    model.getUsedAt(),
                    model.getCreatedAt());
        }
    }
}
