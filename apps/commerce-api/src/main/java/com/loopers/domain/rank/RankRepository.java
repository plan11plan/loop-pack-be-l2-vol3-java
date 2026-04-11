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

    long countByRankingDate(LocalDate rankingDate);

    Optional<Long> findRankByProductIdAndRankingDate(Long productId, LocalDate rankingDate);
}
