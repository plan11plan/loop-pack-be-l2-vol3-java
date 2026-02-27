package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderModel 단위 테스트")
class OrderModelTest {

    @DisplayName("생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 값이면 ORDERED 상태로 생성되고 totalPrice가 items로부터 계산된다")
        @Test
        void create_withValidValues() {
            // arrange
            List<OrderItemModel> items = List.of(
                    OrderItemModel.create(10L, 25000, 2, "상품A", "브랜드A"));

            // act
            OrderModel order = OrderModel.create(1L, items);

            // assert
            assertAll(
                    () -> assertThat(order.getUserId()).isEqualTo(1L),
                    () -> assertThat(order.getTotalPrice()).isEqualTo(50000),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED),
                    () -> assertThat(order.getItems()).hasSize(1),
                    () -> assertThat(order.getItems().get(0).getOrder()).isSameAs(order));
        }

        @DisplayName("items가 비어있으면 예외가 발생한다")
        @Test
        void create_withEmptyItems_throwsException() {
            assertThatThrownBy(() -> OrderModel.create(1L, List.of()))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("userId가 null이면 예외가 발생한다")
        @Test
        void create_withNullUserId_throwsException() {
            // arrange
            List<OrderItemModel> items = List.of(
                    OrderItemModel.create(10L, 25000, 1, "상품A", "브랜드A"));

            // act & assert
            assertThatThrownBy(() -> OrderModel.create(null, items))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("소유자 검증할 때, ")
    @Nested
    class ValidateOwner {

        @DisplayName("본인 주문이 아니면 예외가 발생한다")
        @Test
        void validateOwner_notOwner_throwsException() {
            // arrange
            OrderModel order = OrderModel.create(1L, List.of(
                    OrderItemModel.create(10L, 25000, 1, "상품A", "브랜드A")));

            // act & assert
            assertThatThrownBy(() -> order.validateOwner(999L))
                    .isInstanceOf(CoreException.class);
        }
    }
}
