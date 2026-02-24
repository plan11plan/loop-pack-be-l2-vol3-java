package com.loopers.domain.order.dto;

import java.util.List;

public class OrderCommand {

    public record Create(Long userId, List<CreateItem> items) {

        public record CreateItem(
            Long productId,
            int orderPrice,
            int quantity,
            String productName,
            String brandName
        ) {}
    }
}
