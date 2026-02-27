package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

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

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    private List<OrderItemModel> items = new ArrayList<>();

    private OrderModel(Long userId, int totalPrice, OrderStatus status) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public static OrderModel create(Long userId, List<OrderItemModel> items) {
        validateUserId(userId);
        validateItems(items);
        OrderModel order = new OrderModel(userId, 0, OrderStatus.ORDERED);
        items.forEach(order::addItem);
        order.totalPrice = order.calculateTotalPrice();
        return order;
    }

    public void addItem(OrderItemModel item) {
        items.add(item);
        item.assignOrder(this);
    }

    public int calculateTotalPrice() {
        return items.stream()
                .mapToInt(item -> item.getOrderPrice() * item.getQuantity())
                .sum();
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

    private static void validateItems(List<OrderItemModel> items) {
        if (items == null || items.isEmpty()) {
            throw new CoreException(OrderErrorCode.EMPTY_ORDER_ITEMS);
        }
    }
}
