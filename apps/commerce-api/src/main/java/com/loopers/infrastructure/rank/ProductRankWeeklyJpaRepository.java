package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.ProductRankWeeklyEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ProductRankWeeklyJpaRepository
        extends JpaRepository<ProductRankWeeklyEntity, Long> {

    List<ProductRankWeeklyEntity> findByYearMonthWeekOrderByRankValueAsc(
            String yearMonthWeek, Pageable pageable);

    List<ProductRankWeeklyEntity> findByYearMonthWeekOrderByRankValueAsc(String yearMonthWeek);

    long countByYearMonthWeek(String yearMonthWeek);

    @Transactional
    @Modifying
    @Query("DELETE FROM ProductRankWeeklyEntity e WHERE e.yearMonthWeek = :periodKey")
    void deleteByYearMonthWeek(String periodKey);
}
