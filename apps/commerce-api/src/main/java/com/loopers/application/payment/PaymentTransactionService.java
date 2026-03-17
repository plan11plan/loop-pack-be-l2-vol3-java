package com.loopers.application.payment;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.OrderModel;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.dto.OrderCommand;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentErrorCode;
import com.loopers.domain.payment.PaymentModel;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.dto.ProductInfo;
import com.loopers.support.error.CoreException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private static final String PG_PROVIDER = "PG_SIMULATOR";

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ProductService productService;
    private final BrandService brandService;
    private final CouponService couponService;

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 10,
            backoff = @Backoff(delay = 50, random = true))
    @Transactional
    public PaymentModel createOrderAndPayment(Long userId, PaymentCriteria.Create criteria) {
        List<ProductInfo.StockDeduction> deductionInfos =
                productService.validateAndDeductStock(criteria.toStockDeductions());

        Map<Long, String> brandNameMap = brandService.getNameMapByIds(
                ProductInfo.StockDeduction.extractDistinctBrandIds(deductionInfos));

        OrderModel order = orderService.createOrder(
                userId, OrderCommand.CreateItem.from(deductionInfos, brandNameMap));

        if (criteria.couponId() != null) {
            orderService.applyDiscount(
                    order,
                    (int) couponService.useAndCalculateDiscount(
                            criteria.couponId(), userId, order.getId(),
                            order.getOriginalTotalPrice()));
        }

        return paymentService.createPayment(
                order.getId(), order.getTotalPrice(),
                criteria.cardType(), maskCardNo(criteria.cardNo()), PG_PROVIDER);
    }

    @Transactional
    public PaymentModel createOrRetryPayment(Long orderId, CardType cardType, String cardNo) {
        OrderModel order = orderService.getByIdWithLock(orderId);
        if (!order.isPendingPayment()) {
            throw new CoreException(PaymentErrorCode.INVALID_ORDER_STATUS);
        }

        String maskedCardNo = maskCardNo(cardNo);

        if (paymentService.findByOrderId(orderId).isEmpty()) {
            return paymentService.createPayment(
                    orderId, order.getTotalPrice(),
                    cardType, maskedCardNo, PG_PROVIDER);
        }
        return paymentService.retryPayment(
                orderId, cardType, maskedCardNo, PG_PROVIDER);
    }

    @Transactional
    public void savePaymentKey(Long orderId, String paymentKey) {
        paymentRepository.findByOrderId(orderId)
                .ifPresent(payment -> {
                    if (!payment.getTransactions().isEmpty()) {
                        payment.getTransactions()
                                .get(payment.getTransactions().size() - 1)
                                .assignPaymentKey(paymentKey);
                    }
                });
    }

    @Transactional
    public void failLastTransaction(Long orderId, String failureCode, String failureMessage) {
        paymentRepository.findByOrderId(orderId)
                .ifPresent(payment -> {
                    if (!payment.getTransactions().isEmpty()) {
                        payment.getTransactions()
                                .get(payment.getTransactions().size() - 1)
                                .fail(failureCode, failureMessage);
                    }
                });
    }

    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 4) {
            return cardNo;
        }
        return "****-****-****-" + cardNo.substring(cardNo.length() - 4);
    }
}
