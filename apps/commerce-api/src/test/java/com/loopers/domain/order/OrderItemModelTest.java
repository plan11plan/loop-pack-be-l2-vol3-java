package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderItemModel 단위 테스트")
class OrderItemModelTest {

    @DisplayName("생성")
    @Nested
    class Create {

        @DisplayName("유효한 값이면 생성에 성공한다 (스냅샷 포함)")
        @Test
        void create_withValidValues() {
            // act
            OrderItemModel orderItem = OrderItemModel.create(1L, 10L, 25000, 2, "상품A", "브랜드A");

            // assert
            assertAll(
                () -> assertThat(orderItem.getOrderId()).isEqualTo(1L),
                () -> assertThat(orderItem.getProductId()).isEqualTo(10L),
                () -> assertThat(orderItem.getOrderPrice().getValue()).isEqualTo(25000),
                () -> assertThat(orderItem.getQuantity().getValue()).isEqualTo(2),
                () -> assertThat(orderItem.getProductSnapshot().getProductName()).isEqualTo("상품A"),
                () -> assertThat(orderItem.getProductSnapshot().getBrandName()).isEqualTo("브랜드A")
            );
        }

        @DisplayName("orderId가 null이면 예외가 발생한다")
        @Test
        void create_withNullOrderId_throwsException() {
            // act & assert
            assertThatThrownBy(() -> OrderItemModel.create(null, 10L, 25000, 2, "상품A", "브랜드A"))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("productId가 null이면 예외가 발생한다")
        @Test
        void create_withNullProductId_throwsException() {
            // act & assert
            assertThatThrownBy(() -> OrderItemModel.create(1L, null, 25000, 2, "상품A", "브랜드A"))
                .isInstanceOf(CoreException.class);
        }
    }
}
