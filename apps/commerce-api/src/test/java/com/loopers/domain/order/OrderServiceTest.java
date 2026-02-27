package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

    private OrderService orderService;
    private FakeOrderRepository fakeOrderRepository;
    private FakeOrderItemRepository fakeOrderItemRepository;

    @BeforeEach
    void setUp() {
        fakeOrderRepository = new FakeOrderRepository();
        fakeOrderItemRepository = new FakeOrderItemRepository();
        orderService = new OrderService(fakeOrderRepository, fakeOrderItemRepository);
    }

    private List<OrderItemModel> createSampleItems() {
        return List.of(
                OrderItemModel.create(10L, 25000, 2, "상품A", "브랜드A"));
    }

    @DisplayName("주문을 생성할 때, ")
    @Nested
    class CreateOrder {

        @DisplayName("주문이 저장되고 totalPrice가 계산된다")
        @Test
        void createOrder_savesOrder() {
            // act
            OrderModel order = orderService.createOrder(1L, createSampleItems());

            // assert
            assertAll(
                    () -> assertThat(order.getId()).isNotEqualTo(0L),
                    () -> assertThat(order.getUserId()).isEqualTo(1L),
                    () -> assertThat(order.getTotalPrice()).isEqualTo(50000),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED),
                    () -> assertThat(order.getItems()).hasSize(1));
        }

        @DisplayName("빈 항목이면 예외가 발생한다")
        @Test
        void createOrder_withEmptyItems_throwsException() {
            assertThatThrownBy(() -> orderService.createOrder(1L, List.of()))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("주문을 조회할 때, ")
    @Nested
    class GetOrder {

        @DisplayName("ID로 주문을 조회한다")
        @Test
        void getById_returnsOrder() {
            // arrange
            OrderModel savedOrder = orderService.createOrder(1L, createSampleItems());

            // act
            OrderModel foundOrder = orderService.getById(savedOrder.getId());

            // assert
            assertThat(foundOrder.getId()).isEqualTo(savedOrder.getId());
        }

        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생한다")
        @Test
        void getById_notFound_throwsException() {
            assertThatThrownBy(() -> orderService.getById(999L))
                    .isInstanceOf(CoreException.class);
        }

        @DisplayName("ID + userId로 본인 주문을 조회한다")
        @Test
        void getByIdAndUserId_returnsOrder() {
            // arrange
            OrderModel savedOrder = orderService.createOrder(1L, createSampleItems());

            // act
            OrderModel foundOrder = orderService.getByIdAndUserId(savedOrder.getId(), 1L);

            // assert
            assertThat(foundOrder.getId()).isEqualTo(savedOrder.getId());
        }

        @DisplayName("본인 주문이 아니면 예외가 발생한다")
        @Test
        void getByIdAndUserId_notOwner_throwsException() {
            // arrange
            OrderModel savedOrder = orderService.createOrder(1L, createSampleItems());

            // act & assert
            assertThatThrownBy(() -> orderService.getByIdAndUserId(savedOrder.getId(), 999L))
                    .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("유저ID + 기간으로 주문 목록을 조회할 때, ")
    @Nested
    class GetOrdersByUserIdAndPeriod {

        @DisplayName("해당 유저의 주문 목록을 반환한다")
        @Test
        void getOrdersByUserIdAndPeriod_returnsOrders() {
            // arrange
            orderService.createOrder(1L, createSampleItems());

            // act
            List<OrderModel> orders = orderService.getOrdersByUserIdAndPeriod(
                    1L,
                    java.time.ZonedDateTime.now().minusDays(1),
                    java.time.ZonedDateTime.now().plusDays(1));

            // assert
            assertThat(orders).hasSize(1);
        }
    }

    @DisplayName("전체 주문을 페이지네이션으로 조회할 때, ")
    @Nested
    class GetAllOrders {

        @DisplayName("전체 주문을 페이지네이션으로 반환한다")
        @Test
        void getAllOrders_returnsPage() {
            // arrange
            orderService.createOrder(1L, createSampleItems());
            orderService.createOrder(2L, List.of(
                    OrderItemModel.create(20L, 30000, 1, "상품B", "브랜드B")));

            // act
            Page<OrderModel> page = orderService.getAllOrders(PageRequest.of(0, 10));

            // assert
            assertAll(
                    () -> assertThat(page.getTotalElements()).isEqualTo(2),
                    () -> assertThat(page.getContent()).hasSize(2));
        }
    }

    @DisplayName("아이템을 취소할 때, ")
    @Nested
    class CancelItem {

        @DisplayName("주문을 조회하고 아이템을 취소한다")
        @Test
        void cancelItem_success() {
            // arrange
            OrderModel order = orderService.createOrder(1L, createSampleItems());
            Long orderItemId = order.getItems().get(0).getId();

            // act
            OrderItemModel cancelledItem = orderService.cancelItem(order.getId(), orderItemId);

            // assert
            assertAll(
                    () -> assertThat(cancelledItem.getStatus()).isEqualTo(OrderItemStatus.CANCELLED),
                    () -> assertThat(order.getTotalPrice()).isEqualTo(0),
                    () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED));
        }
    }
}
