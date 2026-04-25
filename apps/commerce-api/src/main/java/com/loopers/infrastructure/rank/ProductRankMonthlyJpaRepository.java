package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.ProductRankMonthlyEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ProductRankMonthlyJpaRepository
        extends JpaRepository<ProductRankMonthlyEntity, Long> {

    List<ProductRankMonthlyEntity> findByYearMonthOrderByRankValueAsc(
            String yearMonth, Pageable pageable);

    List<ProductRankMonthlyEntity> findByYearMonthOrderByRankValueAsc(String yearMonth);

    long countByYearMonth(String yearMonth);

    @Transactional
    @Modifying
    @Query("DELETE FROM ProductRankMonthlyEntity e WHERE e.yearMonth = :periodKey")
    void deleteByYearMonth(String periodKey);
}
