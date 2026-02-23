package com.loopers.application.like;

import com.loopers.application.like.dto.LikeCriteria;
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
    public void toggleLike(LikeCriteria.Toggle criteria) {
        ProductModel product = productService.getById(criteria.productId());

        switch (criteria.type()) {
            case LIKE -> {
                productLikeService.like(criteria.userId(), criteria.productId());
                product.addLikeCount();
            }
            case UNLIKE -> {
                productLikeService.unlike(criteria.userId(), criteria.productId());
                product.subtractLikeCount();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<LikeResult> getMyLikedProducts(Long userId) {
        return productLikeService.getLikesByUserId(userId).stream()
            .filter(like -> productService.existsById(like.getProductId()))
            .map(LikeResult::from)
            .toList();
    }
}
