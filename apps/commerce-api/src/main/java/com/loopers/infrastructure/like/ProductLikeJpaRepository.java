package com.loopers.infrastructure.like;

import com.loopers.domain.like.ProductLikeModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductLikeJpaRepository extends JpaRepository<ProductLikeModel,Long> {
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);

    List<ProductLikeModel> findAllByUserId(Long userId);
}
