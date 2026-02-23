package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductLikeService {
    private final ProductLikeRepository productLikeRepository;

    @Transactional
    public boolean toggleLike(Long userId, Long productId) {
        Optional<ProductLikeModel> existingLike =
                productLikeRepository.findByUserIdAndProductId(userId, productId);

        if (existingLike.isPresent()) {
            productLikeRepository.delete(existingLike.get());
            return false;
        }

        productLikeRepository.save(ProductLikeModel.create(userId, productId));
        return true;
    }

    @Transactional(readOnly = true)
    public List<ProductLikeModel> getLikesByUserId(Long userId) {
        return productLikeRepository.findAllByUserId(userId);
    }
}
