package com.loopers.application.like.dto;

import com.loopers.domain.like.ProductLikeModel;
import java.time.ZonedDateTime;
import java.util.List;

public record LikeInfo(Long id, Long userId, Long productId, ZonedDateTime createdAt) {
    public static LikeInfo from(ProductLikeModel model) {
        return new LikeInfo(model.getId(), model.getUserId(), model.getProductId(), model.getCreatedAt());
    }
    public static List<LikeInfo> from(List<ProductLikeModel> models) {
        return models.stream()
                .map(model ->
                        new LikeInfo(model.getId(), model.getUserId(), model.getProductId(), model.getCreatedAt()))
                        .toList();
    }
}
