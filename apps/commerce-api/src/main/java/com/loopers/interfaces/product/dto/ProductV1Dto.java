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
        int stock,
        long likeCount,
        String thumbnailUrl,
        List<ImageResponse> mainImages,
        List<ImageResponse> detailImages
    ) {
        public static DetailResponse from(ProductResult.DetailWithImages detail) {
            return new DetailResponse(
                    detail.product().id(),
                    detail.product().brandId(),
                    detail.product().brandName(),
                    detail.product().name(),
                    detail.product().price(),
                    detail.product().stock(),
                    detail.product().likeCount(),
                    detail.product().thumbnailUrl(),
                    detail.mainImages().stream().map(ImageResponse::from).toList(),
                    detail.detailImages().stream().map(ImageResponse::from).toList());
        }
    }

    public record ImageResponse(Long id, String imageUrl, String imageType, int sortOrder) {
        public static ImageResponse from(ProductResult.ImageResult image) {
            return new ImageResponse(
                    image.id(), image.imageUrl(), image.imageType().name(), image.sortOrder());
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
            int price,
            long likeCount,
            String thumbnailUrl
        ) {
            public static ListItem from(ProductResult info) {
                return new ListItem(
                        info.id(),
                        info.brandId(),
                        info.brandName(),
                        info.name(),
                        info.price(),
                        info.likeCount(),
                        info.thumbnailUrl());
            }
        }
    }
}
