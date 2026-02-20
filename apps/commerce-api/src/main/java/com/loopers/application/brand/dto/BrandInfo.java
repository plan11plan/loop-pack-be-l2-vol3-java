package com.loopers.application.brand.dto;

import com.loopers.domain.brand.BrandModel;

public record BrandInfo(Long id, String name) {
    public static BrandInfo from(BrandModel model) {
        return new BrandInfo(model.getId(), model.getName());
    }
}
