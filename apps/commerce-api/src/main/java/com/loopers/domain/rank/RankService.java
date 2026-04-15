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

    @Transactional(readOnly = true)
    public List<RankInfo.RankedScore> getTopRankedByDate(LocalDate date, Pageable pageable) {
        return RankInfo.RankedScore.fromScoresWithRank(
                rankingScoreRepository.findTopByRankingDateOrderByScoreDesc(date, pageable),
                (int) pageable.getOffset());
    }

    @Transactional(readOnly = true)
    public List<RankInfo.RankedScore> getTopRankedByDate(
            String version, LocalDate date, Pageable pageable) {
        return RankInfo.RankedScore.fromScoresWithRank(
                rankingScoreRepository.findTopByRankingDateOrderByScoreDesc(version, date, pageable),
                (int) pageable.getOffset());
    }

    @Transactional(readOnly = true)
    public long countByDate(LocalDate date) {
        return rankingScoreRepository.countByRankingDate(date);
    }

    @Transactional(readOnly = true)
    public long countByDate(String version, LocalDate date) {
        return rankingScoreRepository.countByRankingDate(version, date);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getRankByProductIdAndDate(Long productId, LocalDate date) {
        return rankingScoreRepository.findRankByProductIdAndRankingDate(productId, date);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getRankByProductIdAndDate(
            String version, Long productId, LocalDate date) {
        return rankingScoreRepository.findRankByProductIdAndRankingDate(version, productId, date);
    }
}
