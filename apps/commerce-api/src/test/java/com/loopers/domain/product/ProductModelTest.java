package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.brand.BrandModel;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductModelTest {

    private BrandModel createBrand() {
        return BrandModel.create("Nike");
    }

    @DisplayName("상품을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        void create_whenValidValues() {
            // act
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);

            // assert
            assertThat(product.getName()).isEqualTo("에어맥스");
            assertThat(product.getPrice().getValue()).isEqualTo(150000);
            assertThat(product.getStock().getValue()).isEqualTo(100);
            assertThat(product.getLikeCount()).isEqualTo(0);
            assertThat(product.getBrand().getName()).isEqualTo("Nike");
        }

        @DisplayName("브랜드가 null이면 예외가 발생한다.")
        @Test
        void create_whenBrandIsNull() {
            assertThatThrownBy(() -> ProductModel.create(null, "에어맥스", 150000, 100))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("브랜드는 필수값입니다.");
        }

        @DisplayName("상품명이 null이면 예외가 발생한다.")
        @Test
        void create_whenNameIsNull() {
            assertThatThrownBy(() -> ProductModel.create(createBrand(), null, 150000, 100))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품명은 필수값입니다.");
        }

        @DisplayName("상품명이 빈 문자열이면 예외가 발생한다.")
        @Test
        void create_whenNameIsBlank() {
            assertThatThrownBy(() -> ProductModel.create(createBrand(), "  ", 150000, 100))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품명은 필수값입니다.");
        }

        @DisplayName("상품명이 100자 이상이면 예외가 발생한다.")
        @Test
        void create_whenNameTooLong() {
            String longName = "a".repeat(100);

            assertThatThrownBy(() -> ProductModel.create(createBrand(), longName, 150000, 100))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품명은 99자 이하여야 합니다.");
        }

        @DisplayName("가격이 음수이면 예외가 발생한다.")
        @Test
        void create_whenPriceIsNegative() {
            assertThatThrownBy(() -> ProductModel.create(createBrand(), "에어맥스", -1, 100))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("가격은 0 이상이어야 합니다.");
        }

        @DisplayName("재고가 음수이면 예외가 발생한다.")
        @Test
        void create_whenStockIsNegative() {
            assertThatThrownBy(() -> ProductModel.create(createBrand(), "에어맥스", 150000, -1))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고는 0 이상이어야 합니다.");
        }
    }

    @DisplayName("상품을 수정할 때, ")
    @Nested
    class Update {

        @DisplayName("유효한 값이 주어지면, 정상적으로 수정된다.")
        @Test
        void update_whenValidValues() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);

            // act
            product.update("에어포스", 120000, 50);

            // assert
            assertThat(product.getName()).isEqualTo("에어포스");
            assertThat(product.getPrice().getValue()).isEqualTo(120000);
            assertThat(product.getStock().getValue()).isEqualTo(50);
        }

        @DisplayName("상품명이 null이면 예외가 발생한다.")
        @Test
        void update_whenNameIsNull() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);

            // act & assert
            assertThatThrownBy(() -> product.update(null, 120000, 50))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품명은 필수값입니다.");
        }
    }

    @DisplayName("재고를 차감할 때, ")
    @Nested
    class DecreaseStock {

        @DisplayName("충분한 재고가 있으면 정상적으로 차감된다.")
        @Test
        void decreaseStock_whenEnough() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 10);

            // act
            product.decreaseStock(3);

            // assert
            assertThat(product.getStock().getValue()).isEqualTo(7);
        }

        @DisplayName("재고가 부족하면 예외가 발생한다.")
        @Test
        void decreaseStock_whenInsufficient() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 5);

            // act & assert
            assertThatThrownBy(() -> product.decreaseStock(6))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고가 부족합니다.");
        }
    }

    @DisplayName("품절 여부를 확인할 때, ")
    @Nested
    class IsSoldOut {

        @DisplayName("재고가 0이면 품절이다.")
        @Test
        void isSoldOut_whenStockIsZero() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 0);

            // act & assert
            assertThat(product.isSoldOut()).isTrue();
        }

        @DisplayName("재고가 있으면 품절이 아니다.")
        @Test
        void isSoldOut_whenStockExists() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 1);

            // act & assert
            assertThat(product.isSoldOut()).isFalse();
        }
    }

    @DisplayName("좋아요 수를 변경할 때, ")
    @Nested
    class LikeCount {

        @DisplayName("addLikeCount() 호출 시 1 증가한다.")
        @Test
        void addLikeCount() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);

            // act
            product.addLikeCount();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("subtractLikeCount() 호출 시 1 감소한다.")
        @Test
        void subtractLikeCount() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);
            product.addLikeCount();
            product.addLikeCount();

            // act
            product.subtractLikeCount();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("좋아요 수가 0일 때 subtractLikeCount() 호출 시 0을 유지한다.")
        @Test
        void subtractLikeCount_whenZero() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);

            // act
            product.subtractLikeCount();

            // assert
            assertThat(product.getLikeCount()).isEqualTo(0);
        }
    }

    @DisplayName("상품을 삭제할 때, ")
    @Nested
    class Delete {

        @DisplayName("delete() 호출 시 deletedAt이 설정된다.")
        @Test
        void delete_whenCalled() {
            // arrange
            ProductModel product = ProductModel.create(createBrand(), "에어맥스", 150000, 100);

            // act
            product.delete();

            // assert
            assertThat(product.getDeletedAt()).isNotNull();
        }
    }
}
