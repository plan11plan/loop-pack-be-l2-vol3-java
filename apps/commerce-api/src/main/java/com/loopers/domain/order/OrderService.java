package com.loopers.domain.order;

import com.loopers.domain.order.dto.OrderCommand;
import com.loopers.support.error.CoreException;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderModel createOrder(OrderCommand.Create command) {
        validateItems(command.items());

        int totalPrice = command.items().stream()
            .mapToInt(item -> item.orderPrice() * item.quantity())
            .sum();

        OrderModel savedOrder = orderRepository.save(
                OrderModel.create(command.userId(), totalPrice));

        for (OrderCommand.Create.CreateItem item : command.items()) {
            orderItemRepository.save(
                    OrderItemModel.create(
                            savedOrder.getId(),
                            item.productId(),
                            item.orderPrice(),
                            item.quantity(),
                            item.productName(),
                            item.brandName()));
        }

        return savedOrder;
    }

    @Transactional(readOnly = true)
    public OrderModel getById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new CoreException(OrderErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public OrderModel getByIdAndUserId(Long id, Long userId) {
        OrderModel order = getById(id);
        order.validateOwner(userId);
        return order;
    }

    @Transactional(readOnly = true)
    public List<OrderModel> getOrdersByUserIdAndPeriod(Long userId, ZonedDateTime startAt, ZonedDateTime endAt) {
        return orderRepository.findAllByUserIdAndCreatedAtBetween(userId, startAt, endAt);
    }

    @Transactional(readOnly = true)
    public List<OrderItemModel> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Page<OrderModel> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    private void validateItems(List<OrderCommand.Create.CreateItem> items) {
        if (items == null || items.isEmpty()) {
            throw new CoreException(OrderErrorCode.EMPTY_ORDER_ITEMS);
        }
    }
}
