package com.loopers.domain.like;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeProductLikeRepository implements ProductLikeRepository {

    private final Map<Long, ProductLikeModel> store = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public ProductLikeModel save(ProductLikeModel productLike) {
        if (productLike.getId() == null) {
            try {
                var idField = productLike.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(productLike, idGenerator.getAndIncrement());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        store.put(productLike.getId(), productLike);
        return productLike;
    }

    @Override
    public Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId) {
        return store.values().stream()
            .filter(like -> like.getUserId().equals(userId) && like.getProductId().equals(productId))
            .findFirst();
    }

    @Override
    public void delete(ProductLikeModel productLike) {
        store.remove(productLike.getId());
    }
}
