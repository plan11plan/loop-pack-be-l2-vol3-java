package com.loopers.application.product;

import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnlikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeCountHandler {
    private final ProductService productService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductLikedEvent event) {
        try {
            productService.incrementLikeCount(event.productId());
        } catch (Exception e) {
            log.warn("[LikeCount] 집계 증가 실패 — productId={}", event.productId(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductUnlikedEvent event) {
        try {
            productService.decrementLikeCount(event.productId());
        } catch (Exception e) {
            log.warn("[LikeCount] 집계 감소 실패 — productId={}", event.productId(), e);
        }
    }
}
