package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.product.dto.ProductCommand;
import java.util.List;

public class PaymentCriteria {

    public record Create(
            Long orderId,
            List<OrderItem> items,
            Long couponId,
            CardType cardType,
            String cardNo) {

        public boolean isNewOrder() {
            return orderId == null;
        }

        public List<ProductCommand.StockDeduction> toStockDeductions() {
            return items.stream()
                    .map(item -> new ProductCommand.StockDeduction(
                            item.productId(), item.quantity(), item.expectedPrice()))
                    .toList();
        }

        public record OrderItem(Long productId, int quantity, int expectedPrice) {}
    }
}
