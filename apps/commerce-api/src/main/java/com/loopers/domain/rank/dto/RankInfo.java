package com.loopers.domain.rank.dto;

import com.loopers.domain.rank.RankModel;
import java.util.ArrayList;
import java.util.List;

public class RankInfo {

    public record RankedScore(long rank, Long productId, double score) {

        public static List<RankedScore> fromScoresWithRank(
                List<RankModel> scores, int offset) {
            List<RankedScore> result = new ArrayList<>();
            long currentRank = offset + 1;
            Double previousScore = null;

            for (int i = 0; i < scores.size(); i++) {
                RankModel score = scores.get(i);
                if (previousScore == null || score.getScore() != previousScore) {
                    currentRank = offset + i + 1;
                }
                previousScore = score.getScore();
                result.add(new RankedScore(
                        currentRank, score.getProductId(), score.getScore()));
            }
            return result;
        }
    }
}
