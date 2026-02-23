package com.loopers.application.like.dto;

import com.loopers.domain.like.dto.LikeCommand;

public class LikeCriteria {

    public record Toggle(LikeCommand.ApplyLikeRequestType type, Long userId, Long productId) {
        public LikeCommand.Toggle toCommand() {
            return new LikeCommand.Toggle(type, userId, productId);
        }
    }
}
