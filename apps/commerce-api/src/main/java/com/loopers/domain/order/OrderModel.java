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

    @Column(name = "original_total_price", nullable = false)
    private int originalTotalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "order", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<OrderItemModel> items = new ArrayList<>();

    @Column(name = "discount_amount", nullable = false)
    private int discountAmount;

    private OrderModel(Long userId, int totalPrice, OrderStatus status) {
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.originalTotalPrice = totalPrice;
        this.status = status;
        this.discountAmount = 0;
    }

    public static OrderModel create(Long userId, List<OrderItemModel> items) {
        return create(userId, items, 0);
    }

    public static OrderModel create(Long userId, List<OrderItemModel> items,
                                    int discountAmount) {
        validateUserId(userId);
        validateItems(items);
        OrderModel order = new OrderModel(userId, 0, OrderStatus.ORDERED);
        items.forEach(order::addItem);
        int calculatedPrice = order.calculateTotalPrice();
        order.originalTotalPrice = calculatedPrice;
        order.discountAmount = discountAmount;
        order.totalPrice = calculatedPrice - discountAmount;
        return order;
    }

    public void addItem(OrderItemModel item) {
        items.add(item);
        item.assignOrder(this);
    }

    public OrderItemModel cancelItem(Long orderItemId) {
        if (this.status == OrderStatus.CANCELLED) {
            throw new CoreException(OrderErrorCode.ALREADY_CANCELLED_ORDER);
        }
        OrderItemModel item = items.stream()
                .filter(i -> i.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new CoreException(OrderErrorCode.ORDER_ITEM_NOT_FOUND));
        item.cancel();
        recalculateTotalPrice();
        if (isAllItemsCancelled()) {
            this.status = OrderStatus.CANCELLED;
        }
        return item;
    }

    private boolean isAllItemsCancelled() {
        return items.stream()
                .allMatch(item -> item.getStatus() == OrderItemStatus.CANCELLED);
    }

    public int calculateTotalPrice() {
        return OrderItemModel.calculateTotalPrice(items);
    }

    public void recalculateTotalPrice() {
        this.totalPrice = items.stream()
                .filter(item -> item.getStatus() == OrderItemStatus.ORDERED)
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
