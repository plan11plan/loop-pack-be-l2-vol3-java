package com.loopers.domain.rank;

public record RankWeightVersion(
        String versionKey,
        double viewWeight,
        double likeWeight,
        double orderWeight) {

    public double computeScore(long viewCount, long likeCount, long orderCount) {
        return viewCount * viewWeight
                + likeCount * likeWeight
                + orderCount * orderWeight;
    }
}
