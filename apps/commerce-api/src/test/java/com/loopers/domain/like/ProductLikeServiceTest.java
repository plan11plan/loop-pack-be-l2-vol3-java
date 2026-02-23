//package com.loopers.domain.like;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//
//class ProductLikeServiceTest {
//
//    private ProductLikeService productLikeService;
//    private FakeProductLikeRepository productLikeRepository;
//
//    @BeforeEach
//    void setUp() {
//        productLikeRepository = new FakeProductLikeRepository();
//        productLikeService = new ProductLikeService(productLikeRepository);
//    }
//
//    @DisplayName("좋아요를 토글할 때, ")
//    @Nested
//    class ToggleLike {
//
//        @DisplayName("좋아요가 없으면, 좋아요를 등록하고 true를 반환한다.")
//        @Test
//        void toggleLike_whenNotExists() {
//            // arrange
//            Long userId = 1L;
//            Long productId = 2L;
//
//            // act
//            boolean result = productLikeService.toggleLike(userId, productId);
//
//            // assert
//            assertThat(result).isTrue();
//            assertThat(productLikeRepository.findByUserIdAndProductId(userId, productId)).isPresent();
//        }
//    }
//}
