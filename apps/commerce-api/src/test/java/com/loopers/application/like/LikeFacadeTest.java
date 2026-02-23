package com.loopers.application.like;

import static com.loopers.application.like.dto.LikeCommand.ApplyLikeRequestType.LIKE;
import static com.loopers.application.like.dto.LikeCommand.ApplyLikeRequestType.UNLIKE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.application.like.dto.LikeCommand;
import com.loopers.application.like.dto.LikeInfo;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.like.ProductLikeModel;
import com.loopers.domain.like.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("LikeFacade 단위 테스트")
@ExtendWith(MockitoExtension.class)
class LikeFacadeTest {

    @Mock
    private ProductLikeService productLikeService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private LikeFacade likeFacade;

    @DisplayName("좋아요를 토글할 때, ")
    @Nested
    class ToggleLike {

        @DisplayName("LIKE 요청이면, 상품을 검증하고 like를 호출하고 likeCount를 증가시킨다.")
        @Test
        void toggleLike_whenLike_addsLikeAndIncreasesCount() {
            // arrange
            LikeCommand.Toggle command = new LikeCommand.Toggle(LIKE, 1L, 2L);
            ProductModel product = ProductModel.create(
                BrandModel.create("Nike"), "에어맥스", 150000, 100
            );
            when(productService.getById(2L)).thenReturn(product);

            // act
            likeFacade.toggleLike(command);

            // assert
            verify(productService).getById(2L);
            verify(productLikeService).like(1L, 2L);
            assertThat(product.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("UNLIKE 요청이면, 상품을 검증하고 unlike를 호출하고 likeCount를 감소시킨다.")
        @Test
        void toggleLike_whenUnlike_removesLikeAndDecreasesCount() {
            // arrange
            LikeCommand.Toggle command = new LikeCommand.Toggle(UNLIKE, 1L, 2L);
            ProductModel product = ProductModel.create(
                BrandModel.create("Nike"), "에어맥스", 150000, 100
            );
            product.addLikeCount(); // likeCount = 1
            when(productService.getById(2L)).thenReturn(product);

            // act
            likeFacade.toggleLike(command);

            // assert
            verify(productService).getById(2L);
            verify(productLikeService).unlike(1L, 2L);
            assertThat(product.getLikeCount()).isEqualTo(0);
        }
    }

    @DisplayName("좋아요 목록을 조회할 때, ")
    @Nested
    class GetLikesByUserId {

        @DisplayName("사용자의 좋아요 목록을 조회하면, LikeInfo 목록을 반환한다.")
        @Test
        void getLikesByUserId_returnsLikeInfoList() {
            // arrange
            Long userId = 1L;
            List<ProductLikeModel> likes = List.of(
                ProductLikeModel.create(userId, 10L),
                ProductLikeModel.create(userId, 20L)
            );
            when(productLikeService.getLikesByUserId(userId)).thenReturn(likes);
            when(productService.existsById(10L)).thenReturn(true);
            when(productService.existsById(20L)).thenReturn(true);

            // act
            List<LikeInfo> result = likeFacade.getMyLikedProducts(userId);

            // assert
            assertThat(result).hasSize(2);
        }

        @DisplayName("삭제된 상품의 좋아요는 목록에서 제외된다.")
        @Test
        void getLikesByUserId_excludesDeletedProducts() {
            // arrange
            Long userId = 1L;
            List<ProductLikeModel> likes = List.of(
                ProductLikeModel.create(userId, 10L),
                ProductLikeModel.create(userId, 20L)
            );
            when(productLikeService.getLikesByUserId(userId)).thenReturn(likes);
            when(productService.existsById(10L)).thenReturn(true);
            when(productService.existsById(20L)).thenReturn(false);

            // act
            List<LikeInfo> result = likeFacade.getMyLikedProducts(userId);

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo(10L);
        }
    }
}
