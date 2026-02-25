package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderModel extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    private OrderModel(Long userId, int totalPrice, OrderStatus status) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public static OrderModel create(Long userId, int totalPrice) {
        validateUserId(userId);
        validateTotalPrice(totalPrice);
        return new OrderModel(userId, totalPrice, OrderStatus.ORDERED);
    }

    public void validateOwner(Long userId) {
        if (!userId.equals(this.userId)) {
            throw new CoreException(OrderErrorCode.FORBIDDEN);
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수값입니다.");
        }
    }

    private static void validateTotalPrice(int totalPrice) {
        if (totalPrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.");
        }
    }
}
