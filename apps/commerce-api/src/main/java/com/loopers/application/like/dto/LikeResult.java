package com.loopers.application.like.dto;

import com.loopers.domain.like.ProductLikeModel;
import java.time.ZonedDateTime;
import java.util.List;

public record LikeResult(Long id, Long userId, Long productId, ZonedDateTime createdAt) {
    public static LikeResult from(ProductLikeModel model) {
        return new LikeResult(model.getId(), model.getUserId(), model.getProductId(), model.getCreatedAt());
    }
    public static List<LikeResult> from(List<ProductLikeModel> models) {
        return models.stream()
                .map(model ->
                        new LikeResult(model.getId(), model.getUserId(), model.getProductId(), model.getCreatedAt()))
                        .toList();
    }
}
