package com.loopers.interfaces.like.dto;

import com.loopers.application.like.dto.LikeResult;
import java.time.ZonedDateTime;
import java.util.List;

public class LikeV1Dto {

    public record LikeResponse(
        Long productId,
        ZonedDateTime createdAt
    ) {
        public static LikeResponse from(LikeResult result) {
            return new LikeResponse(result.productId(), result.createdAt());
        }
    }

    public record ListResponse(
        List<ListItem> items
    ) {
        public record ListItem(
            Long productId,
            ZonedDateTime createdAt
        ) {
            public static ListItem from(LikeResult result) {
                return new ListItem(result.productId(), result.createdAt());
            }
        }
    }
}
