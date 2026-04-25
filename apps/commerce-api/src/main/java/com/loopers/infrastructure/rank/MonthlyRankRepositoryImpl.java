package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.MonthlyRankRepository;
import com.loopers.domain.rank.dto.RankInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MonthlyRankRepositoryImpl implements MonthlyRankRepository {

    private final ProductRankMonthlyJpaRepository jpaRepository;

    @Override
    public List<RankInfo.RankedScore> findTop(String periodKey, Pageable pageable) {
        return jpaRepository.findByYearMonthOrderByRankValueAsc(periodKey, pageable)
                .stream()
                .map(e -> new RankInfo.RankedScore(e.getRankValue(), e.getProductId(), e.getScore()))
                .toList();
    }

    @Override
    public long count(String periodKey) {
        return jpaRepository.countByYearMonth(periodKey);
    }
}
