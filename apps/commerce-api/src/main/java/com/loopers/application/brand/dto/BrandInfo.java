package com.loopers.application.brand.dto;

import com.loopers.domain.brand.BrandModel;
import java.time.ZonedDateTime;

public record BrandInfo(Long id, String name, ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime deletedAt) {
    public static BrandInfo from(BrandModel model) {
        return new BrandInfo(model.getId(), model.getName(), model.getCreatedAt(), model.getUpdatedAt(), model.getDeletedAt());
    }
}
