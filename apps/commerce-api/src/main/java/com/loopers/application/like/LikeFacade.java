package com.loopers.application.like;

import com.loopers.application.like.dto.LikeCommand.Toggle;
import com.loopers.application.like.dto.LikeInfo;
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
    public void toggleLike(Toggle command) {
        ProductModel product = productService.getById(command.productId());

        switch (command.type()) {
            case LIKE -> {
                productLikeService.like(command.userId(), command.productId());
                product.addLikeCount();
            }
            case UNLIKE -> {
                productLikeService.unlike(command.userId(), command.productId());
                product.subtractLikeCount();
            }
        }
    }

    @Transactional(readOnly = true)
    public List<LikeInfo> getMyLikedProducts(Long userId) {
        return productLikeService.getLikesByUserId(userId).stream()
            .filter(like -> productService.existsById(like.getProductId()))
            .map(LikeInfo::from)
            .toList();
    }
}
