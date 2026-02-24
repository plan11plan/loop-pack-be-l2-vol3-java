package com.loopers.application.order.dto;

import com.loopers.domain.order.OrderItemModel;
import com.loopers.domain.order.OrderModel;
import java.time.ZonedDateTime;
import java.util.List;

public class OrderResult {

    public record OrderSummary(
        Long orderId,
        int totalPrice,
        String status,
        ZonedDateTime createdAt
    ) {
        public static OrderSummary from(OrderModel model) {
            return new OrderSummary(
                    model.getId(),
                    model.getTotalPrice().getValue(),
                    model.getStatus().name(),
                    model.getCreatedAt());
        }
    }

    public record OrderDetail(
        Long orderId,
        Long userId,
        int totalPrice,
        String status,
        ZonedDateTime createdAt,
        List<OrderItemDetail> items
    ) {
        public static OrderDetail from(OrderModel model, List<OrderItemModel> items) {
            return new OrderDetail(
                    model.getId(),
                    model.getUserId(),
                    model.getTotalPrice().getValue(),
                    model.getStatus().name(),
                    model.getCreatedAt(),
                    items.stream().map(OrderItemDetail::from).toList());
        }
    }

    public record OrderItemDetail(
        Long orderItemId,
        Long productId,
        String productName,
        String brandName,
        int orderPrice,
        int quantity
    ) {
        public static OrderItemDetail from(OrderItemModel model) {
            return new OrderItemDetail(
                    model.getId(),
                    model.getProductId(),
                    model.getProductSnapshot().getProductName(),
                    model.getProductSnapshot().getBrandName(),
                    model.getOrderPrice().getValue(),
                    model.getQuantity().getValue());
        }
    }
}
