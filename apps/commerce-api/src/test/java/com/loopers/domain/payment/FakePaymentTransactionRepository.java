package com.loopers.domain.payment;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakePaymentTransactionRepository implements PaymentTransactionRepository {

    private final Map<Long, PaymentTransactionModel> store = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public PaymentTransactionModel save(PaymentTransactionModel transaction) {
        if (transaction.getId() == null || transaction.getId() == 0L) {
            setId(transaction, idGenerator.getAndIncrement());
        }
        store.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<PaymentTransactionModel> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<PaymentTransactionModel> findByPaymentKey(String paymentKey) {
        return store.values().stream()
                .filter(t -> paymentKey.equals(t.getPaymentKey()))
                .findFirst();
    }

    @Override
    public List<PaymentTransactionModel> findByPaymentId(Long paymentId) {
        return store.values().stream()
                .filter(t -> t.getPayment().getId().equals(paymentId))
                .toList();
    }

    private void setId(PaymentTransactionModel transaction, Long id) {
        try {
            Field field = transaction.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(transaction, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
