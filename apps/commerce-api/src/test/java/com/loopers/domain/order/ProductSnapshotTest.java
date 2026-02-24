package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProductSnapshot VO 단위 테스트")
class ProductSnapshotTest {

    @DisplayName("생성")
    @Nested
    class Create {

        @DisplayName("유효한 값이면 생성에 성공한다")
        @Test
        void create_withValidValues() {
            // act
            ProductSnapshot snapshot = new ProductSnapshot("상품A", "브랜드A");

            // assert
            assertAll(
                () -> assertThat(snapshot.getProductName()).isEqualTo("상품A"),
                () -> assertThat(snapshot.getBrandName()).isEqualTo("브랜드A")
            );
        }

        @DisplayName("상품명이 null이면 예외가 발생한다")
        @Test
        void create_withNullProductName_throwsException() {
            // act & assert
            assertThatThrownBy(() -> new ProductSnapshot(null, "브랜드A"))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("브랜드명이 null이면 예외가 발생한다")
        @Test
        void create_withNullBrandName_throwsException() {
            // act & assert
            assertThatThrownBy(() -> new ProductSnapshot("상품A", null))
                .isInstanceOf(CoreException.class);
        }
    }
}
