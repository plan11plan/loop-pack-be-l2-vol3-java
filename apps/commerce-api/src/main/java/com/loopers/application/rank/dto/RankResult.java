package com.loopers.application.rank.dto;

import com.loopers.domain.product.ProductModel;
import com.loopers.domain.rank.dto.RankInfo;
import java.time.LocalDate;
import java.util.List;

public class RankResult {

    public record RankingEntry(
            long rank,
            Long productId,
            String productName,
            String brandName,
            int price,
            String thumbnailUrl,
            double score) {

        public static RankingEntry of(RankInfo.RankedScore ranked,
                ProductModel product, String brandName) {
            return new RankingEntry(
                    ranked.rank(),
                    product.getId(),
                    product.getName(),
                    brandName,
                    product.getPrice(),
                    product.getThumbnailUrl(),
                    ranked.score());
        }
    }

    public record RankingPage(
            LocalDate date,
            LocalDate endDate,
            String periodKey,
            int page,
            int size,
            long totalElements,
            List<RankingEntry> items) {
    }
}
