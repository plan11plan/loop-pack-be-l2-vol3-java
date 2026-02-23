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

    @DisplayName("좋아요를 등록할 때, ")
    @Nested
    class Like {

        @DisplayName("유효한 userId와 productId가 주어지면, 좋아요가 저장된다.")
        @Test
        void like_whenValidValues() {
            // act
            productLikeService.like(1L, 2L);

            // assert
            assertThat(productLikeRepository.findByUserIdAndProductId(1L, 2L)).isPresent();
        }
    }

    @DisplayName("좋아요를 취소할 때, ")
    @Nested
    class Unlike {

        @DisplayName("존재하는 좋아요가 주어지면, 좋아요가 삭제된다.")
        @Test
        void unlike_whenExists() {
            // arrange
            productLikeRepository.save(ProductLikeModel.create(1L, 2L));

            // act
            productLikeService.unlike(1L, 2L);

            // assert
            assertThat(productLikeRepository.findByUserIdAndProductId(1L, 2L)).isEmpty();
        }
    }

    @DisplayName("좋아요 존재 여부를 확인할 때, ")
    @Nested
    class ExistsByUserIdAndProductId {

        @DisplayName("좋아요가 존재하면, true를 반환한다.")
        @Test
        void exists_whenLikeExists() {
            // arrange
            productLikeRepository.save(ProductLikeModel.create(1L, 2L));

            // act & assert
            assertThat(productLikeService.existsByUserIdAndProductId(1L, 2L)).isTrue();
        }

        @DisplayName("좋아요가 없으면, false를 반환한다.")
        @Test
        void exists_whenLikeNotExists() {
            // act & assert
            assertThat(productLikeService.existsByUserIdAndProductId(1L, 2L)).isFalse();
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
            productLikeRepository.save(ProductLikeModel.create(2L, 30L));

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
