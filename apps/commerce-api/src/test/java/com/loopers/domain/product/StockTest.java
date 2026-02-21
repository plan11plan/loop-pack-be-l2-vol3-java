package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StockTest {

    @DisplayName("Stock을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("0 이상의 값이면 정상적으로 생성된다.")
        @Test
        void create_whenValidValue() {
            // act
            Stock stock = new Stock(100);

            // assert
            assertThat(stock.getValue()).isEqualTo(100);
        }

        @DisplayName("0이면 정상적으로 생성된다.")
        @Test
        void create_whenZero() {
            // act
            Stock stock = new Stock(0);

            // assert
            assertThat(stock.getValue()).isEqualTo(0);
        }

        @DisplayName("음수이면 예외가 발생한다.")
        @Test
        void create_whenNegative() {
            assertThatThrownBy(() -> new Stock(-1))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고는 0 이상이어야 합니다.");
        }
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    class Deduct {

        @DisplayName("충분한 재고가 있으면 정상적으로 차감된다.")
        @Test
        void deduct_whenEnoughStock() {
            // arrange
            Stock stock = new Stock(10);

            // act
            stock.deduct(3);

            // assert
            assertThat(stock.getValue()).isEqualTo(7);
        }

        @DisplayName("재고가 부족하면 예외가 발생한다.")
        @Test
        void deduct_whenInsufficientStock() {
            // arrange
            Stock stock = new Stock(5);

            // act & assert
            assertThatThrownBy(() -> stock.deduct(6))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고가 부족합니다.");
        }

        @DisplayName("재고와 동일한 수량을 차감하면 0이 된다.")
        @Test
        void deduct_whenExactStock() {
            // arrange
            Stock stock = new Stock(5);

            // act
            stock.deduct(5);

            // assert
            assertThat(stock.getValue()).isEqualTo(0);
        }
    }

    @DisplayName("재고 충분 여부를 확인할 때, ")
    @Nested
    class HasEnough {

        @DisplayName("충분하면 true를 반환한다.")
        @Test
        void hasEnough_whenEnough() {
            // arrange
            Stock stock = new Stock(10);

            // act & assert
            assertThat(stock.hasEnough(10)).isTrue();
        }

        @DisplayName("부족하면 false를 반환한다.")
        @Test
        void hasEnough_whenNotEnough() {
            // arrange
            Stock stock = new Stock(5);

            // act & assert
            assertThat(stock.hasEnough(6)).isFalse();
        }
    }
}
