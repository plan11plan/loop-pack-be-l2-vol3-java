package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "owned_coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OwnedCouponModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private CouponModel coupon;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OwnedCouponStatus status;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "used_at")
    private ZonedDateTime usedAt;

    private OwnedCouponModel(CouponModel coupon, Long userId) {
        this.coupon = coupon;
        this.userId = userId;
        this.status = OwnedCouponStatus.AVAILABLE;
    }

    public static OwnedCouponModel create(CouponModel coupon, Long userId) {
        return new OwnedCouponModel(coupon, userId);
    }

    public void use(Long userId) {
        validateUsable(userId);
        this.status = OwnedCouponStatus.USED;
        this.usedAt = ZonedDateTime.now();
    }

    public void assignOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void restore() {
        if (this.coupon.getExpiredAt().isBefore(ZonedDateTime.now())) {
            this.status = OwnedCouponStatus.EXPIRED;
        } else {
            this.status = OwnedCouponStatus.AVAILABLE;
        }
        this.orderId = null;
        this.usedAt = null;
    }

    public void validateUsable(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new CoreException(CouponErrorCode.NOT_OWNED);
        }
        if (this.status != OwnedCouponStatus.AVAILABLE) {
            throw new CoreException(CouponErrorCode.ALREADY_USED);
        }
        if (this.coupon.getExpiredAt().isBefore(ZonedDateTime.now())) {
            throw new CoreException(CouponErrorCode.EXPIRED);
        }
    }
}
