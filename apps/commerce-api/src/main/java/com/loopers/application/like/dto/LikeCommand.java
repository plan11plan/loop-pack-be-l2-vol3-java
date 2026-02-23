package com.loopers.application.like.dto;

public class LikeCommand {

    public enum ApplyLikeRequestType {
        LIKE,
        UNLIKE
    }
    public record Toggle(ApplyLikeRequestType type, Long userId, Long productId) {
    }

}
