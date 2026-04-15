package com.loopers.domain.order.event;

import java.util.List;

public record OrderCompletedEvent(
        Long orderId, Long userId, int totalPrice, List<Long> productIds) {
}
