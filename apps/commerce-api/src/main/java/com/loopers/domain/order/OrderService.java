package com.loopers.domain.order;

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
    public OrderModel createOrder(Long userId, List<OrderItemModel> items) {
        return orderRepository.save(OrderModel.create(userId, items));
    }

    @Transactional
    public OrderModel createOrder(Long userId, List<OrderItemModel> items,
                                  Long ownedCouponId, int discountAmount) {
        return orderRepository.save(OrderModel.create(userId, items, ownedCouponId, discountAmount));
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

    @Transactional
    public OrderItemModel cancelItem(Long orderId, Long orderItemId) {
        OrderModel order = getById(orderId);
        return order.cancelItem(orderItemId);
    }

    @Transactional(readOnly = true)
    public List<OrderItemModel> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findAllByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Page<OrderModel> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }
}
