package com.loopers.application.rank;

import com.loopers.application.rank.dto.RankResult;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.rank.RankService;
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

    @Transactional(readOnly = true)
    public RankResult.RankingPage getTopRankings(LocalDate date, Pageable pageable) {
        // 1. Redis ZSET에서 해당 날짜의 랭킹 점수 목록 + 전체 개수 조회
        List<RankInfo.RankedScore> rankedScores = rankService
                .getTopRankedByDate(date, pageable);
        long totalElements = rankService.countByDate(date);

        // 2. 랭킹에 포함된 productId로 상품 정보 일괄 조회 (N+1 방지)
        List<Long> productIds = rankedScores.stream()
                .map(RankInfo.RankedScore::productId)
                .toList();
        Map<Long, ProductModel> productMap = productService
                .findAllByIds(productIds).stream()
                .collect(Collectors.toMap(ProductModel::getId, Function.identity()));

        // 3. 상품이 속한 브랜드 ID를 추출하여 브랜드명 일괄 조회
        List<Long> brandIds = productMap.values().stream()
                .map(ProductModel::getBrandId)
                .distinct()
                .toList();
        Map<Long, String> brandNameMap = brandService.getNameMapByIds(brandIds);

        // 4. 랭킹 점수 + 상품 정보 + 브랜드명을 조합하여 응답 생성
        List<RankResult.RankingEntry> items = rankedScores.stream()
                .filter(ranked -> productMap.containsKey(ranked.productId()))
                .map(ranked -> {
                    ProductModel product = productMap.get(ranked.productId());
                    return RankResult.RankingEntry.of(
                            ranked, product, brandNameMap.get(product.getBrandId()));
                })
                .toList();

        return new RankResult.RankingPage(
                date, pageable.getPageNumber() + 1, pageable.getPageSize(),
                totalElements, items);
    }

    @Transactional(readOnly = true)
    public Optional<Long> getProductRank(Long productId, LocalDate date) {
        return rankService.getRankByProductIdAndDate(productId, date);
    }

    @Transactional
    public void carryOverScores(LocalDate targetDate, double carryOverRate) {
        rankService.carryOver(targetDate, carryOverRate);
    }
}
