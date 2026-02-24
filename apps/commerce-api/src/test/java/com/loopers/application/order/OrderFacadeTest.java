package com.loopers.application.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.application.order.dto.OrderResult;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.order.OrderErrorCode;
import com.loopers.domain.order.OrderItemModel;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.dto.OrderCommand;
import com.loopers.domain.product.ProductErrorCode;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@DisplayName("OrderFacade 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderFacade orderFacade;

    private ProductModel createProductWithId(BrandModel brand, String name, int price, int stock, Long id) {
        ProductModel product = ProductModel.create(brand, name, price, stock);
        try {
            var idField = product.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(product, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return product;
    }

    @DisplayName("주문 생성 (UC-O01)")
    @Nested
    class CreateOrder {

        @DisplayName("상품 일괄 조회 → 가격 검증 → 재고 차감 → 주문 생성 순서를 수행한다")
        @Test
        void createOrder_success() {
            // arrange
            BrandModel brand = BrandModel.create("브랜드A");
            ProductModel product = createProductWithId(brand, "상품A", 25000, 100, 10L);

            when(productService.getAllByIds(List.of(10L))).thenReturn(List.of(product));

            OrderModel order = OrderModel.create(1L, 25000);
            when(orderService.createOrder(any(OrderCommand.Create.class))).thenReturn(order);

            OrderCriteria.Create criteria = new OrderCriteria.Create(List.of(
                new OrderCriteria.Create.CreateItem(10L, 1, 25000)
            ));

            // act
            OrderResult.OrderSummary result = orderFacade.createOrder(1L, criteria);

            // assert
            assertAll(
                () -> verify(productService).getAllByIds(List.of(10L)),
                () -> verify(orderService).createOrder(any(OrderCommand.Create.class)),
                () -> assertThat(result.totalPrice()).isEqualTo(25000)
            );
        }

        @DisplayName("주문 항목이 비어있으면 예외가 발생한다")
        @Test
        void createOrder_withEmptyItems_throwsException() {
            // arrange
            OrderCriteria.Create criteria = new OrderCriteria.Create(List.of());

            when(productService.getAllByIds(List.of())).thenReturn(List.of());
            when(orderService.createOrder(any(OrderCommand.Create.class)))
                .thenThrow(new CoreException(OrderErrorCode.EMPTY_ORDER_ITEMS));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(1L, criteria))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("상품이 존재하지 않으면 예외가 발생한다")
        @Test
        void createOrder_productNotFound_throwsException() {
            // arrange
            when(productService.getAllByIds(List.of(999L)))
                .thenThrow(new CoreException(ProductErrorCode.NOT_FOUND));

            OrderCriteria.Create criteria = new OrderCriteria.Create(List.of(
                new OrderCriteria.Create.CreateItem(999L, 1, 25000)
            ));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(1L, criteria))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("expectedPrice와 현재 가격 불일치 시 예외가 발생한다")
        @Test
        void createOrder_priceMismatch_throwsException() {
            // arrange
            BrandModel brand = BrandModel.create("브랜드A");
            ProductModel product = createProductWithId(brand, "상품A", 25000, 100, 10L);

            when(productService.getAllByIds(List.of(10L))).thenReturn(List.of(product));

            OrderCriteria.Create criteria = new OrderCriteria.Create(List.of(
                new OrderCriteria.Create.CreateItem(10L, 1, 30000)
            ));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(1L, criteria))
                .isInstanceOf(CoreException.class);
        }

        @DisplayName("재고 부족 시 예외가 발생한다")
        @Test
        void createOrder_insufficientStock_throwsException() {
            // arrange
            BrandModel brand = BrandModel.create("브랜드A");
            ProductModel product = createProductWithId(brand, "상품A", 25000, 1, 10L);

            when(productService.getAllByIds(List.of(10L))).thenReturn(List.of(product));

            OrderCriteria.Create criteria = new OrderCriteria.Create(List.of(
                new OrderCriteria.Create.CreateItem(10L, 100, 25000)
            ));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(1L, criteria))
                .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("회원 주문 목록 조회 (UC-O02)")
    @Nested
    class GetMyOrders {

        @DisplayName("기간 내 본인 주문 목록을 반환한다")
        @Test
        void getMyOrders_returnsOrders() {
            // arrange
            OrderModel order = OrderModel.create(1L, 50000);
            ZonedDateTime startAt = ZonedDateTime.now().minusDays(7);
            ZonedDateTime endAt = ZonedDateTime.now();

            when(orderService.getOrdersByUserIdAndPeriod(1L, startAt, endAt))
                .thenReturn(List.of(order));

            OrderCriteria.ListByDate criteria = new OrderCriteria.ListByDate(startAt, endAt);

            // act
            List<OrderResult.OrderSummary> results = orderFacade.getMyOrders(1L, criteria);

            // assert
            assertAll(
                () -> assertThat(results).hasSize(1),
                () -> assertThat(results.get(0).totalPrice()).isEqualTo(50000)
            );
        }
    }

    @DisplayName("회원 주문 상세 조회 (UC-O03)")
    @Nested
    class GetMyOrderDetail {

        @DisplayName("주문 상세 + 주문 항목을 반환한다")
        @Test
        void getMyOrderDetail_returnsDetail() {
            // arrange
            OrderModel order = OrderModel.create(1L, 50000);
            OrderItemModel item = OrderItemModel.create(1L, 10L, 25000, 2, "상품A", "브랜드A");

            when(orderService.getById(1L)).thenReturn(order);
            when(orderService.getOrderItemsByOrderId(1L)).thenReturn(List.of(item));

            // act
            OrderResult.OrderDetail result = orderFacade.getMyOrderDetail(1L, 1L);

            // assert
            assertAll(
                () -> assertThat(result.totalPrice()).isEqualTo(50000),
                () -> assertThat(result.items()).hasSize(1),
                () -> assertThat(result.items().get(0).productName()).isEqualTo("상품A")
            );
        }

        @DisplayName("본인 주문이 아니면 예외가 발생한다")
        @Test
        void getMyOrderDetail_notOwner_throwsException() {
            // arrange
            OrderModel order = OrderModel.create(2L, 50000);
            when(orderService.getById(1L)).thenReturn(order);

            // act & assert
            assertThatThrownBy(() -> orderFacade.getMyOrderDetail(1L, 1L))
                .isInstanceOf(CoreException.class);
        }
    }

    @DisplayName("관리자 주문 조회")
    @Nested
    class AdminOrders {

        @DisplayName("전체 주문 페이지네이션을 반환한다")
        @Test
        void getAllOrders_returnsPage() {
            // arrange
            OrderModel order = OrderModel.create(1L, 50000);
            Page<OrderModel> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);

            when(orderService.getAllOrders(any())).thenReturn(page);

            // act
            Page<OrderResult.OrderSummary> result = orderFacade.getAllOrders(PageRequest.of(0, 10));

            // assert
            assertAll(
                () -> assertThat(result.getTotalElements()).isEqualTo(1),
                () -> assertThat(result.getContent().get(0).totalPrice()).isEqualTo(50000)
            );
        }

        @DisplayName("주문 상세를 반환한다 (소유권 검증 없음)")
        @Test
        void getOrderDetail_returnsDetail_withoutOwnerCheck() {
            // arrange
            OrderModel order = OrderModel.create(2L, 50000);
            OrderItemModel item = OrderItemModel.create(1L, 10L, 25000, 2, "상품A", "브랜드A");

            when(orderService.getById(1L)).thenReturn(order);
            when(orderService.getOrderItemsByOrderId(1L)).thenReturn(List.of(item));

            // act
            OrderResult.OrderDetail result = orderFacade.getOrderDetail(1L);

            // assert
            assertAll(
                () -> assertThat(result.userId()).isEqualTo(2L),
                () -> assertThat(result.items()).hasSize(1)
            );
        }
    }
}
