package com.loopers.domain.rank;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface RankRepository {

    RankModel save(RankModel score);

    Optional<RankModel> findByProductIdAndRankingDate(Long productId, LocalDate rankingDate);

    List<RankModel> findAllByRankingDate(LocalDate rankingDate);

    List<RankModel> findTopByRankingDateOrderByScoreDesc(LocalDate rankingDate, Pageable pageable);

    default List<RankModel> findTopByRankingDateOrderByScoreDesc(
            String version, LocalDate rankingDate, Pageable pageable) {
        return findTopByRankingDateOrderByScoreDesc(rankingDate, pageable);
    }

    long countByRankingDate(LocalDate rankingDate);

    default long countByRankingDate(String version, LocalDate rankingDate) {
        return countByRankingDate(rankingDate);
    }

    Optional<Long> findRankByProductIdAndRankingDate(Long productId, LocalDate rankingDate);

    default Optional<Long> findRankByProductIdAndRankingDate(
            String version, Long productId, LocalDate rankingDate) {
        return findRankByProductIdAndRankingDate(productId, rankingDate);
    }
}
