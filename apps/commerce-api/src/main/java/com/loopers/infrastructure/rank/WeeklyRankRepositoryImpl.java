package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.WeeklyRankRepository;
import com.loopers.domain.rank.dto.RankInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WeeklyRankRepositoryImpl implements WeeklyRankRepository {

    private final ProductRankWeeklyJpaRepository jpaRepository;

    @Override
    public List<RankInfo.RankedScore> findTop(String periodKey, Pageable pageable) {
        return jpaRepository.findByYearMonthWeekOrderByRankValueAsc(periodKey, pageable)
                .stream()
                .map(e -> new RankInfo.RankedScore(e.getRankValue(), e.getProductId(), e.getScore()))
                .toList();
    }

    @Override
    public long count(String periodKey) {
        return jpaRepository.countByYearMonthWeek(periodKey);
    }
}
