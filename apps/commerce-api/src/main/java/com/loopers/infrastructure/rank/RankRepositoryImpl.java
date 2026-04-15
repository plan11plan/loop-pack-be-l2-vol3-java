package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.RankModel;
import com.loopers.domain.rank.RankRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RankRepositoryImpl implements RankRepository {

    private final RankJpaRepository jpaRepository;
    private final RankQdslRepository queryRepository;
    private final RankRedisRepository redisRepository;

    @Override
    public List<RankModel> findTopByRankingDateOrderByScoreDesc(LocalDate rankingDate, Pageable pageable) {
        return redisRepository.findTopByDate(
                rankingDate,
                pageable.getOffset(),
                pageable.getOffset() + pageable.getPageSize() - 1);
    }

    @Override
    public List<RankModel> findTopByRankingDateOrderByScoreDesc(
            String version, LocalDate rankingDate, Pageable pageable) {
        return redisRepository.findTopByDate(
                version, rankingDate,
                pageable.getOffset(),
                pageable.getOffset() + pageable.getPageSize() - 1);
    }

    @Override
    public long countByRankingDate(LocalDate rankingDate) {
        return redisRepository.countByDate(rankingDate);
    }

    @Override
    public long countByRankingDate(String version, LocalDate rankingDate) {
        return redisRepository.countByDate(version, rankingDate);
    }

    @Override
    public Optional<Long> findRankByProductIdAndRankingDate(Long productId, LocalDate rankingDate) {
        return redisRepository.findRankByProductId(productId, rankingDate);
    }

    @Override
    public Optional<Long> findRankByProductIdAndRankingDate(
            String version, Long productId, LocalDate rankingDate) {
        return redisRepository.findRankByProductId(version, productId, rankingDate);
    }

    @Override
    public RankModel save(RankModel score) {
        return jpaRepository.save(score);
    }

    @Override
    public Optional<RankModel> findByProductIdAndRankingDate(Long productId, LocalDate rankingDate) {
        return jpaRepository.findByProductIdAndRankingDate(productId, rankingDate);
    }

    @Override
    public List<RankModel> findAllByRankingDate(LocalDate rankingDate) {
        return queryRepository.findAllByRankingDate(rankingDate);
    }
}
