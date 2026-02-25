package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.order.dto.OrderCommand;
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

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @DisplayName("주문 생성 후 저장된다")
        @Test
        void createOrder_savesOrder() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 2, "상품A", "브랜드A")
            ));

            // act
            OrderModel order = orderService.createOrder(command);

            // assert
            assertAll(
                () -> assertThat(order.getId()).isNotEqualTo(0L),
                () -> assertThat(order.getUserId()).isEqualTo(1L),
                () -> assertThat(order.getTotalPrice()).isEqualTo(50000),
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED)
            );
        }

        @DisplayName("주문 항목이 저장된다")
        @Test
        void createOrder_savesOrderItems() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 2, "상품A", "브랜드A"),
                new OrderCommand.Create.CreateItem(20L, 30000, 1, "상품B", "브랜드B")
            ));

            // act
            OrderModel order = orderService.createOrder(command);

            // assert
            List<OrderItemModel> items = orderService.getOrderItemsByOrderId(order.getId());
            assertAll(
                () -> assertThat(items).hasSize(2),
                () -> assertThat(items.get(0).getProductId()).isEqualTo(10L),
                () -> assertThat(items.get(1).getProductId()).isEqualTo(20L)
            );
        }

        @DisplayName("빈 항목이면 예외가 발생한다")
        @Test
        void createOrder_withEmptyItems_throwsException() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of());

            // act & assert
            assertThatThrownBy(() -> orderService.createOrder(command))
                .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("주문 조회")
    @Nested
    class GetOrder {

        @DisplayName("ID로 주문을 조회한다")
        @Test
        void getById_returnsOrder() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 1, "상품A", "브랜드A")
            ));
            OrderModel savedOrder = orderService.createOrder(command);

            // act
            OrderModel foundOrder = orderService.getById(savedOrder.getId());

            // assert
            assertThat(foundOrder.getId()).isEqualTo(savedOrder.getId());
        }

        @DisplayName("존재하지 않는 주문 조회 시 예외가 발생한다")
        @Test
        void getById_notFound_throwsException() {
            // act & assert
            assertThatThrownBy(() -> orderService.getById(999L))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("ID + userId로 본인 주문을 조회한다")
        @Test
        void getByIdAndUserId_returnsOrder() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 1, "상품A", "브랜드A")
            ));
            OrderModel savedOrder = orderService.createOrder(command);

            // act
            OrderModel foundOrder = orderService.getByIdAndUserId(savedOrder.getId(), 1L);

            // assert
            assertThat(foundOrder.getId()).isEqualTo(savedOrder.getId());
        }

        @DisplayName("본인 주문이 아니면 예외가 발생한다")
        @Test
        void getByIdAndUserId_notOwner_throwsException() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 1, "상품A", "브랜드A")
            ));
            OrderModel savedOrder = orderService.createOrder(command);

            // act & assert
            assertThatThrownBy(() -> orderService.getByIdAndUserId(savedOrder.getId(), 999L))
                .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("유저ID + 기간으로 주문 목록 조회")
    @Nested
    class GetOrdersByUserIdAndPeriod {

        @DisplayName("해당 유저의 주문 목록을 반환한다")
        @Test
        void getOrdersByUserIdAndPeriod_returnsOrders() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 1, "상품A", "브랜드A")
            ));
            orderService.createOrder(command);

            // act
            List<OrderModel> orders = orderService.getOrdersByUserIdAndPeriod(
                1L,
                java.time.ZonedDateTime.now().minusDays(1),
                java.time.ZonedDateTime.now().plusDays(1)
            );

            // assert
            assertThat(orders).hasSize(1);
        }
    }

    @DisplayName("주문ID로 주문 항목 목록 조회")
    @Nested
    class GetOrderItems {

        @DisplayName("해당 주문의 항목 목록을 반환한다")
        @Test
        void getOrderItemsByOrderId_returnsItems() {
            // arrange
            OrderCommand.Create command = new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 2, "상품A", "브랜드A")
            ));
            OrderModel order = orderService.createOrder(command);

            // act
            List<OrderItemModel> items = orderService.getOrderItemsByOrderId(order.getId());

            // assert
            assertThat(items).hasSize(1);
            assertThat(items.get(0).getProductName()).isEqualTo("상품A");
        }
    }

    @DisplayName("전체 주문 페이지네이션 조회")
    @Nested
    class GetAllOrders {

        @DisplayName("전체 주문을 페이지네이션으로 반환한다")
        @Test
        void getAllOrders_returnsPage() {
            // arrange
            orderService.createOrder(new OrderCommand.Create(1L, List.of(
                new OrderCommand.Create.CreateItem(10L, 25000, 1, "상품A", "브랜드A")
            )));
            orderService.createOrder(new OrderCommand.Create(2L, List.of(
                new OrderCommand.Create.CreateItem(20L, 30000, 1, "상품B", "브랜드B")
            )));

            // act
            Page<OrderModel> page = orderService.getAllOrders(PageRequest.of(0, 10));

            // assert
            assertAll(
                () -> assertThat(page.getTotalElements()).isEqualTo(2),
                () -> assertThat(page.getContent()).hasSize(2)
            );
        }
    }
}
