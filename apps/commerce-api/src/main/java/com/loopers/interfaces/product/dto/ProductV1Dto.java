package com.loopers.interfaces.product.dto;

import com.loopers.application.product.dto.ProductResult;
import java.util.List;

public class ProductV1Dto {

    public record DetailResponse(
        Long id,
        Long brandId,
        String brandName,
        String name,
        int price,
        int stock
    ) {
        public static DetailResponse from(ProductResult info) {
            return new DetailResponse(
                    info.id(), info.brandId(), info.brandName(),
                    info.name(), info.price(), info.stock());
        }
    }

    public record ListResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<ListItem> items
    ) {
        public record ListItem(
            Long id,
            Long brandId,
            String brandName,
            String name,
            int price
        ) {
            public static ListItem from(ProductResult info) {
                return new ListItem(
                        info.id(), info.brandId(), info.brandName(),
                        info.name(), info.price());
            }
        }
    }
}
