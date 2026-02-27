package com.loopers.domain.product;

public record ProductSnapshot(Long productId, String name, int price, int quantity, Long brandId) {
}
