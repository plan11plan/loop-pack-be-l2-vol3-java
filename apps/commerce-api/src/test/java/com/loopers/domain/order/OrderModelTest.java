package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderModel 단위 테스트")
class OrderModelTest {

    @DisplayName("생성")
    @Nested
    class Create {

        @DisplayName("유효한 값이면 ORDERED 상태로 생성된다")
        @Test
        void create_withValidValues() {
            // act
            OrderModel order = OrderModel.create(1L, 50000);

            // assert
            assertAll(
                () -> assertThat(order.getUserId()).isEqualTo(1L),
                () -> assertThat(order.getTotalPrice()).isEqualTo(50000),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED)
            );
        }

        @DisplayName("userId가 null이면 예외가 발생한다")
        @Test
        void create_withNullUserId_throwsException() {
            // act & assert
            assertThatThrownBy(() -> OrderModel.create(null, 50000))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("totalPrice가 음수이면 예외가 발생한다")
        @Test
        void create_withNegativeTotalPrice_throwsException() {
            // act & assert
            assertThatThrownBy(() -> OrderModel.create(1L, -1))
                .isInstanceOf(CoreException.class);
        }
    }
}
