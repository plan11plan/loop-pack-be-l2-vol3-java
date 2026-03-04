package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.coupon.CouponDiscountType;
import com.loopers.domain.coupon.CouponModel;
import com.loopers.domain.coupon.OwnedCouponModel;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.coupon.CouponJpaRepository;
import com.loopers.infrastructure.coupon.OwnedCouponJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("주문 동시성 테스트")
@SpringBootTest
class OrderConcurrencyTest {

    private static final int THREAD_COUNT = 30;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

    @Autowired
    private OwnedCouponJpaRepository ownedCouponJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private UserModel createUserWithPoint(String loginId, long point) {
        UserModel user = UserModel.create(
                loginId, "encrypted", "테스트", java.time.LocalDate.of(1990, 1, 1),
                loginId + "@test.com");
        user.addPoint(point);
        return userJpaRepository.save(user);
    }

    private ProductModel createProduct(Long brandId, String name, int price, int stock) {
        return productJpaRepository.save(
                ProductModel.create(brandId, name, price, stock));
    }

    private OwnedCouponModel createOwnedCoupon(Long userId) {
        CouponModel coupon = couponJpaRepository.save(
                CouponModel.create("테스트 쿠폰", CouponDiscountType.FIXED, 5000,
                        null, 100, ZonedDateTime.now().plusMonths(3)));
        coupon.issue();
        couponJpaRepository.save(coupon);
        return ownedCouponJpaRepository.save(OwnedCouponModel.create(coupon, userId));
    }

    @DisplayName("동일 쿠폰 동시 주문:")
    @Nested
    class ConcurrentCouponOrder {

        @DisplayName("여러 스레드에서 같은 OwnedCoupon으로 주문 → 1건만 성공, 나머지 실패")
        @RepeatedTest(3)
        void concurrentOrder_sameCoupon_onlyOneSucceeds() throws InterruptedException {
            // arrange
            BrandModel brand = brandJpaRepository.save(BrandModel.create("브랜드A"));
            Long brandId = brand.getId();
            ProductModel product = createProduct(brandId, "상품A", 10000, 100);

            List<UserModel> users = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                users.add(createUserWithPoint("user" + i, 1_000_000));
            }

            // 모든 유저에게 같은 쿠폰의 OwnedCoupon을 생성 (공유 쿠폰 시뮬레이션)
            // 단, OwnedCoupon은 1인 1개이므로 첫 번째 유저의 쿠폰을 공유
            UserModel firstUser = users.get(0);
            OwnedCouponModel ownedCoupon = createOwnedCoupon(firstUser.getId());

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // act — 모든 스레드가 같은 유저의 같은 쿠폰으로 주문 시도
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        orderFacade.createOrder(firstUser.getId(),
                                new OrderCriteria.Create(List.of(
                                        new OrderCriteria.Create.CreateItem(
                                                product.getId(), 1, 10000)),
                                        ownedCoupon.getId()));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            // assert
            OwnedCouponModel updated = ownedCouponJpaRepository.findById(
                    ownedCoupon.getId()).orElseThrow();

            System.out.println("\n========== 동일 쿠폰 동시 주문 ==========");
            System.out.println("  요청 수(threadCount)   : " + THREAD_COUNT);
            System.out.println("  성공 카운트            : " + successCount.get());
            System.out.println("  실패 카운트            : " + failCount.get());
            System.out.println("  쿠폰 사용 여부         : " + (updated.isUsed() ? "YES" : "NO"));
            System.out.println("  중복 사용 여부          : " + (successCount.get() > 1 ? "DUPLICATED" : "OK"));
            System.out.println("==========================================\n");

            assertAll(
                    () -> assertThat(successCount.get())
                            .as("정확히 1건만 성공해야 한다")
                            .isEqualTo(1),
                    () -> assertThat(failCount.get())
                            .as("나머지는 실패해야 한다")
                            .isEqualTo(THREAD_COUNT - 1),
                    () -> assertThat(updated.isUsed())
                            .as("쿠폰은 사용 상태여야 한다")
                            .isTrue());
        }
    }

    @DisplayName("동일 상품 동시 주문 재고 차감:")
    @Nested
    class ConcurrentStockDeduction {

        @DisplayName("재고 10인 상품에 여러 건 동시 주문 → 재고 정합성 유지, 초과 주문 실패")
        @RepeatedTest(3)
        void concurrentOrder_sameProduct_stockConsistency() throws InterruptedException {
            // arrange
            int initialStock = 10;
            BrandModel brand = brandJpaRepository.save(BrandModel.create("브랜드B"));
            ProductModel product = createProduct(
                    brand.getId(), "한정 상품", 10000, initialStock);

            List<UserModel> users = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                users.add(createUserWithPoint("stockuser" + i, 1_000_000));
            }

            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);
            List<String> errors = Collections.synchronizedList(new ArrayList<>());

            // act — 각 유저가 1개씩 주문 (총 THREAD_COUNT건, 재고는 initialStock)
            for (int i = 0; i < THREAD_COUNT; i++) {
                final UserModel user = users.get(i);
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        startLatch.await();
                        orderFacade.createOrder(user.getId(),
                                new OrderCriteria.Create(List.of(
                                        new OrderCriteria.Create.CreateItem(
                                                product.getId(), 1, 10000))));
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        errors.add(e.getMessage());
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            // assert
            ProductModel updated = productJpaRepository.findById(
                    product.getId()).orElseThrow();

            System.out.println("\n========== 동일 상품 동시 주문 재고 차감 ==========");
            System.out.println("  초기 재고(initialStock): " + initialStock);
            System.out.println("  요청 수(threadCount)   : " + THREAD_COUNT);
            System.out.println("  성공 카운트            : " + successCount.get());
            System.out.println("  실패 카운트            : " + failCount.get());
            System.out.println("  남은 재고              : " + updated.getStock());
            System.out.println("  재고 정합성            : " + (updated.getStock() == initialStock - successCount.get() ? "OK" : "MISMATCH"));
            System.out.println("  초과 차감 여부          : " + (updated.getStock() < 0 ? "OVER-DEDUCTED" : "OK"));
            System.out.println("================================================\n");

            assertAll(
                    () -> assertThat(successCount.get())
                            .as("재고 수만큼만 성공해야 한다")
                            .isEqualTo(initialStock),
                    () -> assertThat(updated.getStock())
                            .as("재고는 0이어야 한다")
                            .isEqualTo(0),
                    () -> assertThat(updated.getStock())
                            .as("재고가 음수가 되면 안 된다")
                            .isGreaterThanOrEqualTo(0));
        }
    }
}
