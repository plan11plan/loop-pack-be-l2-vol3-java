package com.loopers.application.rank;

import com.loopers.application.rank.dto.RankResult;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.rank.MonthlyRankService;
import com.loopers.domain.rank.RankPeriod;
import com.loopers.domain.rank.RankService;
import com.loopers.domain.rank.WeeklyRankService;
import com.loopers.domain.rank.dto.RankInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RankFacade {

    private final RankService rankService;
    private final ProductService productService;
    private final BrandService brandService;
    private final WeeklyRankService weeklyRankService;
    private final MonthlyRankService monthlyRankService;

    @Transactional(readOnly = true)
    public RankResult.RankingPage getTopRankings(LocalDate date, Pageable pageable) {
        String periodKey = RankPeriod.DAILY.toPeriodKey(date);
        return buildRankingPage(
                rankService.getTopRankedByDate(date, pageable),
                rankService.countByDate(date),
                date, RankPeriod.DAILY.toEndDate(date), periodKey, pageable);
    }

    @Transactional(readOnly = true)
    public RankResult.RankingPage getTopRankings(RankPeriod period, String version, LocalDate date, Pageable pageable) {
        String periodKey = period.toPeriodKey(date);
        List<RankInfo.RankedScore> scores = switch (period) {
            case DAILY -> rankService.getTopRankedByDate(version, date, pageable);
            case WEEKLY -> weeklyRankService.findTop(periodKey, pageable);
            case MONTHLY -> monthlyRankService.findTop(periodKey, pageable);
        };
        long total = switch (period) {
            case DAILY -> rankService.countByDate(version, date);
            case WEEKLY -> weeklyRankService.count(periodKey);
            case MONTHLY -> monthlyRankService.count(periodKey);
        };
        return buildRankingPage(scores, total, date, period.toEndDate(date), periodKey, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getProductRank(Long productId, LocalDate date) {
        return rankService.getRankByProductIdAndDate(productId, date);
    }

    private RankResult.RankingPage buildRankingPage(
            List<RankInfo.RankedScore> rankedScores, long totalElements,
            LocalDate date, LocalDate endDate, String periodKey, Pageable pageable) {
        List<Long> productIds = rankedScores.stream()
                .map(RankInfo.RankedScore::productId)
                .toList();
        Map<Long, ProductModel> productMap = productService
                .findAllByIds(productIds).stream()
                .collect(Collectors.toMap(ProductModel::getId, Function.identity()));

        List<Long> brandIds = productMap.values().stream()
                .map(ProductModel::getBrandId)
                .distinct()
                .toList();
        Map<Long, String> brandNameMap = brandService.getNameMapByIds(brandIds);

        List<RankResult.RankingEntry> items = rankedScores.stream()
                .filter(ranked -> productMap.containsKey(ranked.productId()))
                .map(ranked -> {
                    ProductModel product = productMap.get(ranked.productId());
                    return RankResult.RankingEntry.of(
                            ranked, product, brandNameMap.get(product.getBrandId()));
                })
                .toList();

        return new RankResult.RankingPage(
                date, endDate, periodKey,
                pageable.getPageNumber() + 1, pageable.getPageSize(),
                totalElements, items);
    }
}
