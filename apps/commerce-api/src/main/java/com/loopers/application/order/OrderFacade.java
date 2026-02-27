package com.loopers.application.order;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.application.order.dto.OrderResult;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.order.OrderItemModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSnapshot;
import com.loopers.domain.product.StockDeductionCommand;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final OrderService orderService;

    @Transactional
    public OrderResult.OrderSummary createOrder(Long userId, OrderCriteria.Create criteria) {
        List<ProductSnapshot> snapshots = productService.validateAndDeductStock(
                criteria.items().stream()
                        .map(item -> new StockDeductionCommand(
                                item.productId(), item.quantity(), item.expectedPrice()))
                        .toList());

        Map<Long, String> brandNameMap = brandService.getNameMapByIds(
                snapshots.stream()
                        .map(ProductSnapshot::brandId)
                        .distinct()
                        .toList());

        List<OrderItemModel> items = snapshots.stream()
                .map(snapshot -> OrderItemModel.create(
                        snapshot.productId(),
                        snapshot.price(),
                        snapshot.quantity(),
                        snapshot.name(),
                        brandNameMap.get(snapshot.brandId())))
                .toList();

        return OrderResult.OrderSummary.from(
                orderService.createOrder(userId, items));
    }

    @Transactional(readOnly = true)
    public List<OrderResult.OrderSummary> getMyOrders(Long userId, OrderCriteria.ListByDate criteria) {
        return orderService.getOrdersByUserIdAndPeriod(userId, criteria.startAt(), criteria.endAt()).stream()
                .map(OrderResult.OrderSummary::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResult.OrderDetail getMyOrderDetail(Long userId, Long orderId) {
        return OrderResult.OrderDetail.from(
                orderService.getByIdAndUserId(orderId, userId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResult.OrderSummary> getAllOrders(Pageable pageable) {
        return orderService.getAllOrders(pageable)
                .map(OrderResult.OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderResult.OrderDetail getOrderDetail(Long orderId) {
        return OrderResult.OrderDetail.from(orderService.getById(orderId));
    }

    @Transactional
    public void cancelMyOrderItem(Long userId, Long orderId, Long orderItemId) {
        orderService.getByIdAndUserId(orderId, userId);
        OrderItemModel cancelledItem = orderService.cancelItem(orderId, orderItemId);
        productService.increaseStock(cancelledItem.getProductId(), cancelledItem.getQuantity());
    }

    @Transactional
    public void cancelOrderItem(Long orderId, Long orderItemId) {
        OrderItemModel cancelledItem = orderService.cancelItem(orderId, orderItemId);
        productService.increaseStock(cancelledItem.getProductId(), cancelledItem.getQuantity());
    }
}
