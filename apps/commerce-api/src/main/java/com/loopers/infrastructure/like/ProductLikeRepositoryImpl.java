package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLikeModel;
import com.loopers.domain.like.ProductLikeRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductLikeRepositoryImpl implements ProductLikeRepository {
    private final ProductLikeJpaRepository productLikeJpaRepository;
    @Override
    public ProductLikeModel save(ProductLikeModel productLike) {
        return productLikeJpaRepository.save(productLike);
    }

    @Override
    public Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId) {
        return productLikeJpaRepository.findByUserIdAndProductId(userId,productId);
    }

    @Override
    public void delete(ProductLikeModel productLike) {
        productLikeJpaRepository.delete(productLike);
    }

    @Override
    public List<ProductLikeModel> findAllByUserId(Long userId) {
        return productLikeJpaRepository.findAllByUserId(userId);
    }

    @Override
    public long countByProductId(Long productId) {
        return productLikeJpaRepository.countByProductId(productId);
    }
}
