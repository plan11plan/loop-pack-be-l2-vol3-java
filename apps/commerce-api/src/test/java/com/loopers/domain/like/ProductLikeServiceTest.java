package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductLikeServiceTest {

    private ProductLikeService productLikeService;
    private FakeProductLikeRepository productLikeRepository;

    @BeforeEach
    void setUp() {
        productLikeRepository = new FakeProductLikeRepository();
        productLikeService = new ProductLikeService(productLikeRepository);
    }

    @DisplayName("좋아요를 토글할 때, ")
    @Nested
    class ToggleLike {

        @DisplayName("좋아요가 없으면, 좋아요를 등록하고 true를 반환한다.")
        @Test
        void toggleLike_whenNotExists() {
            // arrange
            Long userId = 1L;
            Long productId = 2L;

            // act
            boolean result = productLikeService.toggleLike(userId, productId);

            // assert
            assertThat(result).isTrue();
            assertThat(productLikeRepository.findByUserIdAndProductId(userId, productId)).isPresent();
        }

        @DisplayName("좋아요가 이미 존재하면, 좋아요를 삭제하고 false를 반환한다.")
        @Test
        void toggleLike_whenAlreadyExists() {
            // arrange
            Long userId = 1L;
            Long productId = 2L;
            productLikeRepository.save(ProductLikeModel.create(userId, productId));

            // act
            boolean result = productLikeService.toggleLike(userId, productId);

            // assert
            assertThat(result).isFalse();
            assertThat(productLikeRepository.findByUserIdAndProductId(userId, productId)).isEmpty();
        }
    }

    @DisplayName("좋아요 목록을 조회할 때, ")
    @Nested
    class GetLikesByUserId {

        @DisplayName("사용자 ID로 조회하면, 해당 사용자의 좋아요 목록이 반환된다.")
        @Test
        void getLikesByUserId_whenLikesExist() {
            // arrange
            Long userId = 1L;
            productLikeRepository.save(ProductLikeModel.create(userId, 10L));
            productLikeRepository.save(ProductLikeModel.create(userId, 20L));
            productLikeRepository.save(ProductLikeModel.create(2L, 30L)); // 다른 사용자

            // act
            List<ProductLikeModel> result = productLikeService.getLikesByUserId(userId);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(like -> like.getUserId().equals(userId));
        }

        @DisplayName("좋아요가 없는 사용자 ID로 조회하면, 빈 목록이 반환된다.")
        @Test
        void getLikesByUserId_whenNoLikes() {
            // act
            List<ProductLikeModel> result = productLikeService.getLikesByUserId(999L);

            // assert
            assertThat(result).isEmpty();
        }
    }
}
