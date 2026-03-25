package com.loopers.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaEventMessage;
import com.loopers.confg.kafka.KafkaTopics;
import com.loopers.domain.order.event.OrderCompletedEvent;
import com.loopers.infrastructure.outbox.OutboxEventEntity;
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaPublisher {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        KafkaEventMessage message = KafkaEventMessage.of("ORDER_COMPLETED", Map.of(
                "orderId", event.orderId(),
                "userId", event.userId(),
                "totalPrice", event.totalPrice()));
        try {
            outboxRepository.save(new OutboxEventEntity(
                    message.eventId(), message.eventType(),
                    KafkaTopics.ORDER_EVENTS, String.valueOf(event.orderId()),
                    objectMapper.writeValueAsString(message)));
        } catch (JsonProcessingException e) {
            log.error("[Outbox] JSON 직렬화 실패 — eventType={}", message.eventType(), e);
            throw new RuntimeException(e);
        }
    }
}
