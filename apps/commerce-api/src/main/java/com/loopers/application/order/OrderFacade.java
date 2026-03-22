package com.loopers.application.order;

import com.loopers.application.order.dto.OrderCriteria;
import com.loopers.application.order.dto.OrderResult;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.dto.OrderInfo;
import com.loopers.domain.order.dto.OrderCommand;
import com.loopers.domain.payment.PaymentErrorCode;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PgPaymentRequest;
import com.loopers.domain.payment.PgPaymentResult;
import com.loopers.domain.payment.PgRequestStatus;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductInfo;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
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

    private static final String CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";

    private final ProductService productService;
    private final BrandService brandService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    @Transactional
    public OrderResult.OrderSummary createOrder(Long userId, OrderCriteria.Create criteria) {
        OrderModel order = processOrder(userId, criteria);
        return OrderResult.OrderSummary.from(order);
    }

    @Transactional
    public OrderResult.OrderPaymentSummary createOrderWithPayment(
            Long userId, OrderCriteria.Create criteria) {

        OrderModel order = processOrder(userId, criteria);

        // 결제 요청 (외부 PG API 호출 — 트랜잭션 안에서)
        PaymentModel payment = paymentService.createPending(
                order.getId(), order.getTotalPrice(),
                criteria.cardType(), maskCardNo(criteria.cardNo()));

        PgPaymentResult pgResult = paymentGateway.requestPayment(
                new PgPaymentRequest(
                        String.format("%06d", order.getId()),
                        criteria.cardType().name(),
                        criteria.cardNo(),
                        order.getTotalPrice(),
                        CALLBACK_URL,
                        String.valueOf(userId)));

        if (!pgResult.requested()) {
            paymentService.failById(payment.getId());
            if (pgResult.status() == PgRequestStatus.VALIDATION_ERROR) {
                throw new CoreException(PaymentErrorCode.PG_REQUEST_FAILED, pgResult.pgDetail());
            }
            throw new CoreException(PaymentErrorCode.PG_SERVICE_UNAVAILABLE);
        }

        paymentService.updateRequested(payment.getId(), pgResult.transactionKey());

        return OrderResult.OrderPaymentSummary.from(order, payment);
    }

    private OrderModel processOrder(Long userId, OrderCriteria.Create criteria) {
        // 1. 재고 차감
        List<ProductInfo.StockDeduction> deductionInfos =
                productService.validateAndDeductStock(criteria.toStockDeductions());

        Map<Long, String> brandNameMap = brandService.getNameMapByIds(
                ProductInfo.StockDeduction.extractDistinctBrandIds(deductionInfos));

        // 2. 주문 생성
        OrderModel order = orderService.createOrder(
                userId, OrderCommand.CreateItem.from(deductionInfos, brandNameMap));

        // 3. 쿠폰 사용
        if (criteria.ownedCouponId() != null) {
            orderService.applyDiscount(
                    order,
                    (int) couponService.useAndCalculateDiscount(
                            criteria.ownedCouponId(), userId, order.getId(),
                            order.getOriginalTotalPrice()));
        }

        // 4. 포인트 차감
        userService.deductPoint(userId, order.getTotalPrice());

        return order;
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
        OrderModel order = orderService.getByIdWithLock(orderId);
        order.validateOwner(userId);
        OrderInfo.CancelledItem cancelledItem = orderService.cancelItem(order, orderItemId);
        productService.increaseStock(cancelledItem.productId(), cancelledItem.quantity());
        if (cancelledItem.orderFullyCancelled()) {
            couponService.restoreByOrderId(orderId);
        }
    }

    @Transactional
    public void cancelOrderItem(Long orderId, Long orderItemId) {
        OrderModel order = orderService.getByIdWithLock(orderId);
        OrderInfo.CancelledItem cancelledItem = orderService.cancelItem(order, orderItemId);
        productService.increaseStock(cancelledItem.productId(), cancelledItem.quantity());
        if (cancelledItem.orderFullyCancelled()) {
            couponService.restoreByOrderId(orderId);
        }
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 4) {
            return cardNo;
        }
        return "****-****-****-" + cardNo.substring(cardNo.length() - 4);
    }
}
