package com.loopers.domain.rank;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class RankingScorePolicy {

    private final RankWeightProperties weightProperties;

    public String scoreSqlExpression() {
        RankWeightVersion weight = weightProperties.getDefaultWeightVersion();
        return "(SUM(view_count) * " + weight.viewWeight()
                + " + SUM(like_count) * " + weight.likeWeight()
                + " + SUM(order_count) * " + weight.orderWeight() + ")";
    }

    public double score(long viewCount, long likeCount, long orderCount) {
        return weightProperties.getDefaultWeightVersion()
                .computeScore(viewCount, likeCount, orderCount);
    }
}
