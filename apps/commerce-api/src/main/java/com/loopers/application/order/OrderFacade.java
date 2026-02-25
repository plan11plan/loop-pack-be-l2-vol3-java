package com.loopers.application.order;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.application.order.dto.OrderResult;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandService;
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
    private final BrandService brandService;
    private final OrderService orderService;

    @Transactional
    public OrderResult.OrderSummary createOrder(Long userId, OrderCriteria.Create criteria) {
        List<ProductModel> products = productService.getAllByIds(criteria.items().stream()
                .map(OrderCriteria.Create.CreateItem::productId)
                .toList());

        Map<Long, ProductModel> productMap = products.stream()
                .collect(Collectors.toMap(ProductModel::getId, Function.identity()));

        List<Long> brandIds = products.stream()
                .map(ProductModel::getBrandId)
                .distinct()
                .toList();
        Map<Long, String> brandNameMap = brandService.getAllByIds(brandIds).stream()
                .collect(Collectors.toMap(BrandModel::getId, BrandModel::getName));

        return OrderResult.OrderSummary.from(
                orderService.createOrder(
                        new OrderCommand.Create(userId, criteria.items().stream()
                                .map(item -> {
                                    ProductModel product = productMap.get(item.productId());
                                    product.validateExpectedPrice(item.expectedPrice());
                                    product.decreaseStock(item.quantity());
                                    return new OrderCommand.Create.CreateItem(
                                            item.productId(),
                                            product.getPrice(),
                                            item.quantity(),
                                            product.getName(),
                                            brandNameMap.get(product.getBrandId()));
                                })
                                .toList())));
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
                orderService.getByIdAndUserId(orderId, userId),
                orderService.getOrderItemsByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderResult.OrderSummary> getAllOrders(Pageable pageable) {
        return orderService.getAllOrders(pageable)
                .map(OrderResult.OrderSummary::from);
    }

    @Transactional(readOnly = true)
    public OrderResult.OrderDetail getOrderDetail(Long orderId) {
        return OrderResult.OrderDetail.from(
                orderService.getById(orderId),
                orderService.getOrderItemsByOrderId(orderId));
    }

}
