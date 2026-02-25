package com.loopers.application.product.dto;

import com.loopers.domain.product.ProductModel;
import java.time.ZonedDateTime;

public record ProductResult(
    Long id,
    Long brandId,
    String brandName,
    String name,
    int price,
    int stock,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt,
    ZonedDateTime deletedAt
) {
    public static ProductResult of(ProductModel model, String brandName) {
        return new ProductResult(
                model.getId(),
                model.getBrandId(),
                brandName,
                model.getName(),
                model.getPrice(),
                model.getStock(),
                model.getCreatedAt(),
                model.getUpdatedAt(),
                model.getDeletedAt());
    }
}
