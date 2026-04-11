package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.RankModel;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankJpaRepository extends JpaRepository<RankModel, Long> {

    Optional<RankModel> findByProductIdAndRankingDate(Long productId, LocalDate rankingDate);

    long countByRankingDate(LocalDate rankingDate);
}
