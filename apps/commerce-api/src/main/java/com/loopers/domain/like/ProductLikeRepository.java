package com.loopers.domain.like;

import java.util.Optional;

public interface ProductLikeRepository {
    ProductLikeModel save(ProductLikeModel productLike);

    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);

    void delete(ProductLikeModel productLike);
}
