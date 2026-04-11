package com.loopers.domain.rank;

import com.loopers.domain.rank.dto.RankInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RankService {

    private final RankRepository rankingScoreRepository;

    @Transactional
    public void updateScore(Long productId, LocalDate rankingDate, RankEventType eventType, int amount) {
        double delta = eventType.calculateScore(amount);
        RankModel score = rankingScoreRepository
                .findByProductIdAndRankingDate(productId, rankingDate)
                .orElseGet(() -> rankingScoreRepository.save(
                        RankModel.create(productId, rankingDate, 0)));

        score.addScore(delta);
    }

    @Transactional(readOnly = true)
    public List<RankInfo.RankedScore> getTopRankedByDate(LocalDate date, Pageable pageable) {
        List<RankModel> scores = rankingScoreRepository.findTopByRankingDateOrderByScoreDesc(date, pageable);
        return RankInfo.RankedScore.fromScoresWithRank(
                scores, (int) pageable.getOffset());
    }

    @Transactional(readOnly = true)
    public long countByDate(LocalDate date) {
        return rankingScoreRepository.countByRankingDate(date);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getRankByProductIdAndDate(Long productId, LocalDate date) {
        return rankingScoreRepository.findRankByProductIdAndRankingDate(productId, date);
    }

    @Transactional
    public void carryOver(LocalDate targetDate, double carryOverRate) {
        LocalDate previousDate = targetDate.minusDays(1);
        List<RankModel> previousScores = rankingScoreRepository
                .findAllByRankingDate(previousDate);

        for (RankModel previous : previousScores) {
            if (rankingScoreRepository.findByProductIdAndRankingDate(
                    previous.getProductId(), targetDate).isPresent()) {
                continue;
            }
            rankingScoreRepository.save(
                    previous.createCarriedOver(targetDate, carryOverRate));
        }
    }
}
