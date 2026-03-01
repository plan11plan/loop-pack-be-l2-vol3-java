package com.loopers.domain.coupon;

import com.loopers.domain.BaseEntity;
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
}
