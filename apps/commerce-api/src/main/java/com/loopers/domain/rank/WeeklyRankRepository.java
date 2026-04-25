package com.loopers.domain.rank;

import com.loopers.domain.rank.dto.RankInfo;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface WeeklyRankRepository {

    List<RankInfo.RankedScore> findTop(String periodKey, Pageable pageable);

    long count(String periodKey);
}
