package com.loopers.application.product;

import com.loopers.domain.product.ProductLikeRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeCountReconcileScheduler {
    private static final int DIVISOR = 10;

    private final ProductRepository productRepository;
    private final ProductLikeRepository productLikeRepository;

    private int currentGroup = 0;

    @Scheduled(fixedRate = 180_000)
    @Transactional
    public void reconcile() {
        int group = currentGroup;
        currentGroup = (currentGroup + 1) % DIVISOR;

        try {
            List<ProductModel> products = productRepository.findByIdModulo(DIVISOR, group);
            Map<Long, Long> likeCounts = productLikeRepository
                    .countByProductIdsWithModulo(DIVISOR, group);

            int corrected = 0;
            for (ProductModel product : products) {
                long actualCount = likeCounts.getOrDefault(product.getId(), 0L);
                if (product.getLikeCount() != actualCount) {
                    product.reconcileLikeCount((int) actualCount);
                    corrected++;
                }
            }
            log.info("[LikeCountReconcile] group={}, corrected={}", group, corrected);
        } catch (Exception e) {
            log.warn("[LikeCountReconcile] group={} 재집계 실패", group, e);
        }
    }
}
