package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CouponIssueCounter 단위 테스트")
class CouponIssueCounterTest {

    private CouponIssueCounter counter;

    @BeforeEach
    void setUp() {
        counter = new CouponIssueCounter();
    }

    @DisplayName("tryAcquire할 때, ")
    @Nested
    class TryAcquire {

        @DisplayName("totalQuantity 이하이면 true를 반환한다")
        @Test
        void tryAcquire_withinLimit() {
            assertAll(
                    () -> assertThat(counter.tryAcquire(1L, () -> 3)).isTrue(),
                    () -> assertThat(counter.tryAcquire(1L, () -> 3)).isTrue(),
                    () -> assertThat(counter.tryAcquire(1L, () -> 3)).isTrue());
        }

        @DisplayName("totalQuantity를 초과하면 false를 반환한다")
        @Test
        void tryAcquire_exceedsLimit() {
            // arrange
            counter.tryAcquire(1L, () -> 1);

            // act & assert
            assertThat(counter.tryAcquire(1L, () -> 1)).isFalse();
        }

        @DisplayName("쿠폰별로 독립적인 카운터를 관리한다")
        @Test
        void tryAcquire_independentPerCoupon() {
            // arrange
            counter.tryAcquire(1L, () -> 1); // coupon 1: 1/1

            // act & assert — coupon 2는 별도 카운터
            assertThat(counter.tryAcquire(2L, () -> 1)).isTrue();
        }
    }

    @DisplayName("release할 때, ")
    @Nested
    class Release {

        @DisplayName("카운터를 감소시켜 다른 요청이 통과할 수 있게 한다")
        @Test
        void release_allowsNewAcquire() {
            // arrange
            counter.tryAcquire(1L, () -> 1); // 1/1

            // act
            counter.release(1L); // 0/1

            // assert
            assertThat(counter.tryAcquire(1L, () -> 1)).isTrue();
        }
    }
}
