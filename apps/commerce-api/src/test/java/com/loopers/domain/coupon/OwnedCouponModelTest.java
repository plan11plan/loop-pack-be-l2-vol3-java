package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OwnedCouponModel 단위 테스트")
class OwnedCouponModelTest {

    private CouponModel createCoupon() {
        return CouponModel.create(
                "테스트 쿠폰", CouponDiscountType.FIXED, 5000L,
                null, 1000, ZonedDateTime.now().plusDays(30));
    }

    @DisplayName("생성할 때, ")
    @Nested
    class Create {

        @DisplayName("유효한 값이면 AVAILABLE 상태로 생성된다")
        @Test
        void create_whenValidValues() {
            // arrange
            CouponModel coupon = createCoupon();

            // act
            OwnedCouponModel owned = OwnedCouponModel.create(coupon, 100L);

            // assert
            assertAll(
                    () -> assertThat(owned.getCoupon()).isSameAs(coupon),
                    () -> assertThat(owned.getUserId()).isEqualTo(100L),
                    () -> assertThat(owned.getStatus()).isEqualTo(OwnedCouponStatus.AVAILABLE),
                    () -> assertThat(owned.getUsedAt()).isNull());
        }
    }

    @DisplayName("사용할 때, ")
    @Nested
    class Use {

        @DisplayName("AVAILABLE 상태이고 본인 소유이면 USED로 전이되고 usedAt이 기록된다")
        @Test
        void use_success() {
            // arrange
            OwnedCouponModel owned = OwnedCouponModel.create(createCoupon(), 100L);

            // act
            owned.use(100L);

            // assert
            assertAll(
                    () -> assertThat(owned.getStatus()).isEqualTo(OwnedCouponStatus.USED),
                    () -> assertThat(owned.getUsedAt()).isNotNull());
        }

        @DisplayName("이미 사용된 쿠폰을 재사용하면 예외가 발생한다")
        @Test
        void use_whenAlreadyUsed() {
            // arrange
            OwnedCouponModel owned = OwnedCouponModel.create(createCoupon(), 100L);
            owned.use(100L);

            // act & assert
            assertThatThrownBy(() -> owned.use(100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("이미 사용된 쿠폰입니다.");
        }

        @DisplayName("본인 소유가 아닌 쿠폰을 사용하면 예외가 발생한다")
        @Test
        void use_whenNotOwned() {
            // arrange
            OwnedCouponModel owned = OwnedCouponModel.create(createCoupon(), 100L);

            // act & assert
            assertThatThrownBy(() -> owned.use(999L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("본인 소유의 쿠폰이 아닙니다.");
        }

        @DisplayName("만료된 쿠폰을 사용하면 예외가 발생한다")
        @Test
        void use_whenExpired() {
            // arrange
            CouponModel coupon = CouponModel.create(
                    "테스트 쿠폰", CouponDiscountType.FIXED, 5000L,
                    null, 1000, ZonedDateTime.now().plusDays(1));
            OwnedCouponModel owned = OwnedCouponModel.create(coupon, 100L);
            // 쿠폰 만료 시뮬레이션
            setField(coupon, "expiredAt", ZonedDateTime.now().minusDays(1));

            // act & assert
            assertThatThrownBy(() -> owned.use(100L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("만료된 쿠폰입니다.");
        }
    }

    @DisplayName("복원할 때, ")
    @Nested
    class Restore {

        @DisplayName("유효기간 내이면 AVAILABLE로 복원된다")
        @Test
        void restore_whenNotExpired() {
            // arrange
            OwnedCouponModel owned = OwnedCouponModel.create(createCoupon(), 100L);
            owned.use(100L);

            // act
            owned.restore();

            // assert
            assertAll(
                    () -> assertThat(owned.getStatus()).isEqualTo(OwnedCouponStatus.AVAILABLE),
                    () -> assertThat(owned.getUsedAt()).isNull());
        }

        @DisplayName("만료된 쿠폰이면 EXPIRED로 복원된다")
        @Test
        void restore_whenExpired() {
            // arrange
            CouponModel coupon = CouponModel.create(
                    "테스트 쿠폰", CouponDiscountType.FIXED, 5000L,
                    null, 1000, ZonedDateTime.now().plusDays(1));
            OwnedCouponModel owned = OwnedCouponModel.create(coupon, 100L);
            owned.use(100L);
            // 쿠폰 만료 시뮬레이션
            setField(coupon, "expiredAt", ZonedDateTime.now().minusDays(1));

            // act
            owned.restore();

            // assert
            assertThat(owned.getStatus()).isEqualTo(OwnedCouponStatus.EXPIRED);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
