package com.loopers.application.like;

import com.loopers.application.like.dto.LikeResult;
import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeFacade {
    private final ProductLikeService productLikeService;
    private final ProductService productService;

    @Transactional
    public void like(Long userId, Long productId) {
        ProductModel product = productService.getById(productId);
        productLikeService.like(userId, productId);
        product.addLikeCount();
    }

    @Transactional
    public void unlike(Long userId, Long productId) {
        ProductModel product = productService.getById(productId);
        productLikeService.unlike(userId, productId);
        product.subtractLikeCount();
    }

    @Transactional(readOnly = true)
    public List<LikeResult> getMyLikedProducts(Long userId) {
        return productLikeService.getLikesByUserId(userId).stream()
            .filter(like -> productService.existsById(like.getProductId()))
            .map(LikeResult::from)
            .toList();
    }
}
