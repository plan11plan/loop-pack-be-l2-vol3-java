package com.loopers.application.product.dto;

import com.loopers.domain.product.ProductModel;
import java.time.ZonedDateTime;

public record ProductInfo(
    Long id,
    Long brandId,
    String brandName,
    String name,
    int price,
    int stock,
    int likeCount,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt,
    ZonedDateTime deletedAt
) {
    public static ProductInfo from(ProductModel model) {
        return new ProductInfo(
            model.getId(),
            model.getBrand().getId(),
            model.getBrand().getName(),
            model.getName(),
            model.getPrice().getValue(),
            model.getStock().getValue(),
            model.getLikeCount(),
            model.getCreatedAt(),
            model.getUpdatedAt(),
            model.getDeletedAt()
        );
    }
}
