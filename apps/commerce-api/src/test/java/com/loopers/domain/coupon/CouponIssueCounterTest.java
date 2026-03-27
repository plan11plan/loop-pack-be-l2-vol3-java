package com.loopers.domain.coupon;

import static com.loopers.domain.coupon.CouponIssueCounter.AcquireResult.ALREADY_ISSUED;
import static com.loopers.domain.coupon.CouponIssueCounter.AcquireResult.QUANTITY_EXHAUSTED;
import static com.loopers.domain.coupon.CouponIssueCounter.AcquireResult.SUCCESS;
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

        @DisplayName("totalQuantity 이하이면 SUCCESS를 반환한다")
        @Test
        void tryAcquire_withinLimit() {
            assertAll(
                    () -> assertThat(counter.tryAcquire(1L, 1L, () -> 3)).isEqualTo(SUCCESS),
                    () -> assertThat(counter.tryAcquire(1L, 2L, () -> 3)).isEqualTo(SUCCESS),
                    () -> assertThat(counter.tryAcquire(1L, 3L, () -> 3)).isEqualTo(SUCCESS));
        }

        @DisplayName("totalQuantity를 초과하면 QUANTITY_EXHAUSTED를 반환한다")
        @Test
        void tryAcquire_exceedsLimit() {
            // arrange
            counter.tryAcquire(1L, 1L, () -> 1);

            // act & assert
            assertThat(counter.tryAcquire(1L, 2L, () -> 1)).isEqualTo(QUANTITY_EXHAUSTED);
        }

        @DisplayName("이미 발급된 사용자는 ALREADY_ISSUED를 반환한다")
        @Test
        void tryAcquire_duplicateUser() {
            // arrange
            counter.tryAcquire(1L, 1L, () -> 3);

            // act & assert
            assertThat(counter.tryAcquire(1L, 1L, () -> 3)).isEqualTo(ALREADY_ISSUED);
        }

        @DisplayName("쿠폰별로 독립적인 카운터를 관리한다")
        @Test
        void tryAcquire_independentPerCoupon() {
            // arrange
            counter.tryAcquire(1L, 1L, () -> 1); // coupon 1: 1/1

            // act & assert — coupon 2는 별도 카운터
            assertThat(counter.tryAcquire(2L, 1L, () -> 1)).isEqualTo(SUCCESS);
        }

        @DisplayName("수량 초과로 거절된 사용자는 재시도 시 ALREADY_ISSUED로 즉시 거절된다")
        @Test
        void tryAcquire_afterQuantityExhausted_rejectImmediately() {
            // arrange
            counter.tryAcquire(1L, 1L, () -> 1); // 1/1 성공
            counter.tryAcquire(1L, 2L, () -> 1); // 수량 초과 — set에는 남음

            // act & assert — 재시도: set에 남아있으므로 ALREADY_ISSUED
            assertThat(counter.tryAcquire(1L, 2L, () -> 1)).isEqualTo(ALREADY_ISSUED);
        }
    }

    @DisplayName("release할 때, ")
    @Nested
    class Release {

        @DisplayName("카운터와 사용자 기록을 제거하여 다른 요청이 통과할 수 있게 한다")
        @Test
        void release_allowsNewAcquire() {
            // arrange
            counter.tryAcquire(1L, 1L, () -> 1); // 1/1

            // act
            counter.release(1L, 1L); // 0/1

            // assert
            assertThat(counter.tryAcquire(1L, 2L, () -> 1)).isEqualTo(SUCCESS);
        }

        @DisplayName("release 후 같은 사용자가 다시 발급받을 수 있다")
        @Test
        void release_allowsSameUserRetry() {
            // arrange
            counter.tryAcquire(1L, 1L, () -> 1); // 1/1

            // act
            counter.release(1L, 1L); // 0/1

            // assert
            assertThat(counter.tryAcquire(1L, 1L, () -> 1)).isEqualTo(SUCCESS);
        }
    }
}
