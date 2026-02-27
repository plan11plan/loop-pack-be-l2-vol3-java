package com.loopers.domain.product;

public record StockDeductionCommand(Long productId, int quantity, int expectedPrice) {
}
