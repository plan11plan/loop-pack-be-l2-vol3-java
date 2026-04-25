package com.loopers.domain.rank;

import com.loopers.domain.rank.dto.RankInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeeklyRankService {

    private final WeeklyRankRepository weeklyRankRepository;

    @Transactional(readOnly = true)
    public List<RankInfo.RankedScore> findTop(String periodKey, Pageable pageable) {
        return weeklyRankRepository.findTop(periodKey, pageable);
    }

    @Transactional(readOnly = true)
    public long count(String periodKey) {
        return weeklyRankRepository.count(periodKey);
    }
}
