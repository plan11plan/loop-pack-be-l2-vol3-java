package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductLikeService {
    private final ProductLikeRepository productLikeRepository;

    @Transactional
    public void like(Long userId, Long productId) {
        productLikeRepository.save(ProductLikeModel.create(userId, productId));
    }

    @Transactional
    public void unlike(Long userId, Long productId) {
        ProductLikeModel like = productLikeRepository.findByUserIdAndProductId(userId, productId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA));
        productLikeRepository.delete(like);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserIdAndProductId(Long userId, Long productId) {
        return productLikeRepository.findByUserIdAndProductId(userId, productId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<ProductLikeModel> getLikesByUserId(Long userId) {
        return productLikeRepository.findAllByUserId(userId);
    }
}
