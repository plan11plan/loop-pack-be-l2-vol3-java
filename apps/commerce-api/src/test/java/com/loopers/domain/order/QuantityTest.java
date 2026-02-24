package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Quantity VO 단위 테스트")
class QuantityTest {

    @DisplayName("생성")
    @Nested
    class Create {

        @DisplayName("1 이상이면 생성에 성공한다")
        @Test
        void create_withValidValue() {
            // act
            Quantity quantity = new Quantity(1);

            // assert
            assertThat(quantity.getValue()).isEqualTo(1);
        }

        @DisplayName("0이면 예외가 발생한다")
        @Test
        void create_withZero_throwsException() {
            // act & assert
            assertThatThrownBy(() -> new Quantity(0))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("음수이면 예외가 발생한다")
        @Test
        void create_withNegative_throwsException() {
            // act & assert
            assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(CoreException.class);
        }
    }
}
