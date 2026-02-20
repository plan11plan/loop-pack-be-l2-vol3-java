package com.loopers.interfaces.brand.dto;

import com.loopers.application.brand.dto.BrandInfo;

public class BrandV1Dto {

    public record DetailResponse(
        Long id,
        String name
    ) {
        public static DetailResponse from(BrandInfo info) {
            return new DetailResponse(info.id(), info.name());
        }
    }
}
