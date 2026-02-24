package com.loopers.application.order;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.application.order.dto.OrderResult;
import com.loopers.domain.order.OrderItemModel;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.dto.OrderCommand;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final ProductService productService;
    private final OrderService orderService;

    @Transactional
    public OrderResult.OrderSummary createOrder(Long userId, OrderCriteria.Create criteria) {
        List<Long> productIds = criteria.items().stream()
            .map(OrderCriteria.Create.CreateItem::productId)
            .toList();

        Map<Long, ProductModel> productMap = productService.getAllByIds(productIds).stream()
            .collect(Collectors.toMap(ProductModel::getId, Function.identity()));

        List<OrderCommand.Create.CreateItem> commandItems = criteria.items().stream()
            .map(item -> {
                ProductModel product = productMap.get(item.productId());
                product.validatePrice(item.expectedPrice());
                product.decreaseStock(item.quantity());
                return new OrderCommand.Create.CreateItem(
                    item.productId(),
                    product.getPrice().getValue(),
                    item.quantity(),
                    product.getName(),
                    product.getBrand().getName()
                );
            })
            .toList();

        OrderCommand.Create command = new OrderCommand.Create(userId, commandItems);
        OrderModel order = orderService.createOrder(command);
        return OrderResult.OrderSummary.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResult.OrderSummary> getMyOrders(Long userId, OrderCriteria.ListByDate criteria) {
        List<OrderModel> orders = orderService.getOrdersByUserIdAndPeriod(userId, criteria.startAt(), criteria.endAt());
        return orders.stream()
            .map(OrderResult.OrderSummary::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderResult.OrderDetail getMyOrderDetail(Long userId, Long orderId) {
        OrderModel order = orderService.getByIdAndUserId(orderId, userId);
        List<OrderItemModel> items = orderService.getOrderItemsByOrderId(orderId);
        return OrderResult.OrderDetail.from(order, items);
    }

    @Transactional(readOnly = true)
    public Page<OrderResult.OrderSummary> getAllOrders(Pageable pageable) {
        return orderService.getAllOrders(pageable)
            .map(OrderResult.OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderResult.OrderDetail getOrderDetail(Long orderId) {
        OrderModel order = orderService.getById(orderId);
        List<OrderItemModel> items = orderService.getOrderItemsByOrderId(orderId);
        return OrderResult.OrderDetail.from(order, items);
    }

}
