package com.loopers.application.order;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.application.order.dto.OrderResult;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderItemModel;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductCommand;
import com.loopers.domain.product.dto.ProductInfo;
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
    private final CouponService couponService;

    @Transactional
    public OrderResult.OrderSummary createOrder(Long userId, OrderCriteria.Create criteria) {
        List<ProductInfo.StockDeduction> deductionInfos = productService.validateAndDeductStock(
                criteria.items().stream()
                        .map(item -> new ProductCommand.StockDeduction(
                                item.productId(), item.quantity(), item.expectedPrice()))
                        .toList());

        Map<Long, String> brandNameMap = brandService.getNameMapByIds(
                deductionInfos.stream()
                        .map(ProductInfo.StockDeduction::brandId)
                        .distinct()
                        .toList());

        List<OrderItemModel> items = deductionInfos.stream()
                .map(info -> OrderItemModel.create(
                        info.productId(),
                        info.price(),
                        info.quantity(),
                        info.name(),
                        brandNameMap.get(info.brandId())))
                .toList();

        int discountAmount = 0;
        if (criteria.ownedCouponId() != null) {
            discountAmount = (int) couponService.useAndCalculateDiscount(
                    criteria.ownedCouponId(), userId,
                    OrderItemModel.calculateTotalPrice(items));
        }

        OrderModel order = orderService.createOrder(userId, items, discountAmount);

        if (criteria.ownedCouponId() != null) {
            couponService.linkOrderToCoupon(criteria.ownedCouponId(), order.getId());
        }

        return OrderResult.OrderSummary.from(order);
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
        OrderModel order = orderService.getByIdAndUserId(orderId, userId);
        OrderItemModel cancelledItem = orderService.cancelItem(orderId, orderItemId);
        productService.increaseStock(cancelledItem.getProductId(), cancelledItem.getQuantity());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            couponService.restoreByOrderId(orderId);
        }
    }

    @Transactional
    public void cancelOrderItem(Long orderId, Long orderItemId) {
        OrderModel order = orderService.getById(orderId);
        OrderItemModel cancelledItem = orderService.cancelItem(orderId, orderItemId);
        productService.increaseStock(cancelledItem.getProductId(), cancelledItem.getQuantity());
        if (order.getStatus() == OrderStatus.CANCELLED) {
            couponService.restoreByOrderId(orderId);
        }
    }
}
