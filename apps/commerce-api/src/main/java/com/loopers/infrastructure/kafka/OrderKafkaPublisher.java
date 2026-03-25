package com.loopers.infrastructure.kafka;

import com.loopers.confg.kafka.KafkaEventMessage;
import com.loopers.confg.kafka.KafkaTopics;
import com.loopers.domain.order.event.OrderCompletedEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderKafkaPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        KafkaEventMessage message = KafkaEventMessage.of("ORDER_COMPLETED", Map.of(
                "orderId", event.orderId(),
                "userId", event.userId(),
                "totalPrice", event.totalPrice()));

        kafkaTemplate.send(
                        KafkaTopics.ORDER_EVENTS,
                        String.valueOf(event.orderId()),
                        message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KafkaPublish] 발행 실패 — topic={}, eventId={}",
                                KafkaTopics.ORDER_EVENTS, message.eventId(), ex);
                    } else {
                        log.info("[KafkaPublish] 발행 성공 — topic={}, partition={}, offset={}, eventType={}",
                                KafkaTopics.ORDER_EVENTS,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                message.eventType());
                    }
                });
    }
}
