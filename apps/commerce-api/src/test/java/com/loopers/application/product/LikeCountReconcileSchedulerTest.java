package com.loopers.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.product.ProductLikeRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("LikeCountReconcileScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class LikeCountReconcileSchedulerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductLikeRepository productLikeRepository;

    @InjectMocks
    private LikeCountReconcileScheduler scheduler;

    @DisplayName("좋아요 집계를 재집계할 때, ")
    @Nested
    class Reconcile {

        @DisplayName("불일치 상품의 like_count를 실제 좋아요 수로 보정한다.")
        @Test
        void reconcile_whenMismatch_updatesLikeCount() {
            // arrange
            ProductModel product = ProductModel.create(1L, "상품A", 1000, 10);
            product.addLikeCount();
            product.addLikeCount();
            product.addLikeCount();
            when(productRepository.findByIdModulo(10, 0)).thenReturn(List.of(product));
            when(productLikeRepository.countByProductIdsWithModulo(10, 0))
                    .thenReturn(Map.of(product.getId(), 5L));

            // act
            scheduler.reconcile();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(5);
        }

        @DisplayName("일치하는 상품은 건드리지 않는다.")
        @Test
        void reconcile_whenMatch_doesNotUpdate() {
            // arrange
            ProductModel product = ProductModel.create(1L, "상품A", 1000, 10);
            product.addLikeCount();
            product.addLikeCount();
            product.addLikeCount();
            when(productRepository.findByIdModulo(10, 0)).thenReturn(List.of(product));
            when(productLikeRepository.countByProductIdsWithModulo(10, 0))
                    .thenReturn(Map.of(product.getId(), 3L));

            // act
            scheduler.reconcile();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(3);
        }

        @DisplayName("실행할 때마다 다음 그룹으로 순환한다.")
        @Test
        void reconcile_advancesGroupEachExecution() {
            // arrange
            when(productRepository.findByIdModulo(10, 0)).thenReturn(List.of());
            when(productLikeRepository.countByProductIdsWithModulo(10, 0))
                    .thenReturn(Map.of());
            when(productRepository.findByIdModulo(10, 1)).thenReturn(List.of());
            when(productLikeRepository.countByProductIdsWithModulo(10, 1))
                    .thenReturn(Map.of());

            // act
            scheduler.reconcile();
            scheduler.reconcile();

            // assert
            verify(productRepository).findByIdModulo(10, 0);
            verify(productRepository).findByIdModulo(10, 1);
        }

        @DisplayName("그룹 9 이후 0으로 돌아간다.")
        @Test
        void reconcile_wrapsAroundAfterGroup9() {
            // arrange
            for (int i = 0; i < 10; i++) {
                when(productRepository.findByIdModulo(10, i)).thenReturn(List.of());
                when(productLikeRepository.countByProductIdsWithModulo(10, i))
                        .thenReturn(Map.of());
            }

            // act
            for (int i = 0; i < 11; i++) {
                scheduler.reconcile();
            }

            // assert
            verify(productRepository, times(2)).findByIdModulo(10, 0);
        }

        @DisplayName("좋아요가 없는 상품은 like_count를 0으로 보정한다.")
        @Test
        void reconcile_whenNoLikes_updatesLikeCountToZero() {
            // arrange
            ProductModel product = ProductModel.create(1L, "상품A", 1000, 10);
            product.addLikeCount();
            product.addLikeCount();
            when(productRepository.findByIdModulo(10, 0)).thenReturn(List.of(product));
            when(productLikeRepository.countByProductIdsWithModulo(10, 0))
                    .thenReturn(Map.of());

            // act
            scheduler.reconcile();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(0);
        }

        @DisplayName("예외 발생 시 로그만 남기고 다음 주기를 기다린다.")
        @Test
        void reconcile_whenExceptionOccurs_doesNotThrow() {
            // arrange
            when(productRepository.findByIdModulo(10, 0))
                    .thenThrow(new RuntimeException("DB connection error"));

            // assert
            assertThatCode(() -> scheduler.reconcile()).doesNotThrowAnyException();
        }
    }
}
