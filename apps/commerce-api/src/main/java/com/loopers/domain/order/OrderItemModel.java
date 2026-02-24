package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemModel extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "order_price", nullable = false))
    private Money orderPrice;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
    private Quantity quantity;

    @Embedded
    private ProductSnapshot productSnapshot;

    private OrderItemModel(Long orderId, Long productId, Money orderPrice, Quantity quantity, ProductSnapshot productSnapshot) {
        this.orderId = orderId;
        this.productId = productId;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
        this.productSnapshot = productSnapshot;
    }

    public static OrderItemModel create(
            Long orderId, Long productId, int orderPrice, int quantity,
            String productName, String brandName) {
        validateOrderId(orderId);
        validateProductId(productId);
        return new OrderItemModel(
                orderId,
                productId,
                Money.of(orderPrice),
                Quantity.of(quantity),
                ProductSnapshot.of(productName, brandName));
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수값입니다.");
        }
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수값입니다.");
        }
    }
}
