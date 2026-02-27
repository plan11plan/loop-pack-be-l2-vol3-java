package com.loopers.domain.product.dto;

public class ProductInfo {

    public record StockDeduction(Long productId, String name, int price, int quantity, Long brandId) {}
}
