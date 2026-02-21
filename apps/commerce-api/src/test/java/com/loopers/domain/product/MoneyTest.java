package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @DisplayName("Money를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("0 이상의 값이면 정상적으로 생성된다.")
        @Test
        void create_whenValidValue() {
            // act
            Money money = new Money(10000);

            // assert
            assertThat(money.getValue()).isEqualTo(10000);
        }

        @DisplayName("0이면 정상적으로 생성된다.")
        @Test
        void create_whenZero() {
            // act
            Money money = new Money(0);

            // assert
            assertThat(money.getValue()).isEqualTo(0);
        }

        @DisplayName("음수이면 예외가 발생한다.")
        @Test
        void create_whenNegative() {
            assertThatThrownBy(() -> new Money(-1))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("가격은 0 이상이어야 합니다.");
        }
    }
}
