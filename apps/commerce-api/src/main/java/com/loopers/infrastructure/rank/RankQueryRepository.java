package com.loopers.infrastructure.rank;

import static com.loopers.domain.rank.QRankModel.rankModel;

import com.loopers.domain.rank.RankModel;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RankQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<RankModel> findTopByRankingDateOrderByScoreDesc(
            LocalDate rankingDate, Pageable pageable) {
        return queryFactory
                .selectFrom(rankModel)
                .where(rankModel.rankingDate.eq(rankingDate))
                .orderBy(rankModel.score.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    public Optional<Long> findRankByProductIdAndRankingDate(
            Long productId, LocalDate rankingDate) {
        Double targetScore = queryFactory
                .select(rankModel.score)
                .from(rankModel)
                .where(rankModel.productId.eq(productId)
                        .and(rankModel.rankingDate.eq(rankingDate)))
                .fetchOne();

        if (targetScore == null) {
            return Optional.empty();
        }

        Long rank = queryFactory
                .select(rankModel.count())
                .from(rankModel)
                .where(rankModel.rankingDate.eq(rankingDate)
                        .and(rankModel.score.gt(targetScore)))
                .fetchOne();

        return Optional.of((rank != null ? rank : 0L) + 1);
    }

    public List<RankModel> findAllByRankingDate(LocalDate rankingDate) {
        return queryFactory
                .selectFrom(rankModel)
                .where(rankModel.rankingDate.eq(rankingDate))
                .fetch();
    }
}
