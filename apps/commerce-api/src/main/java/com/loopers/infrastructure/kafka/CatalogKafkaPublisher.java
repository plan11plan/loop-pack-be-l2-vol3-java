package com.loopers.infrastructure.kafka;

import com.loopers.confg.kafka.KafkaEventMessage;
import com.loopers.confg.kafka.KafkaTopics;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnlikedEvent;
import com.loopers.domain.product.event.ProductViewedEvent;
import java.util.HashMap;
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
public class CatalogKafkaPublisher {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductLiked(ProductLikedEvent event) {
        KafkaEventMessage message = KafkaEventMessage.of("PRODUCT_LIKED", Map.of(
                "productId", event.productId(),
                "userId", event.userId()));

        send(KafkaTopics.CATALOG_EVENTS, String.valueOf(event.productId()), message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductUnliked(ProductUnlikedEvent event) {
        KafkaEventMessage message = KafkaEventMessage.of("PRODUCT_UNLIKED", Map.of(
                "productId", event.productId(),
                "userId", event.userId()));

        send(KafkaTopics.CATALOG_EVENTS, String.valueOf(event.productId()), message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductViewed(ProductViewedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", event.productId());
        data.put("userId", event.userId());

        send(KafkaTopics.CATALOG_EVENTS,
                String.valueOf(event.productId()),
                KafkaEventMessage.of("PRODUCT_VIEWED", data));
    }

    private void send(String topic, String key, KafkaEventMessage message) {
        kafkaTemplate.send(topic, key, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KafkaPublish] 발행 실패 — topic={}, eventType={}, eventId={}",
                                topic, message.eventType(), message.eventId(), ex);
                    } else {
                        log.info("[KafkaPublish] 발행 성공 — topic={}, partition={}, offset={}, eventType={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                message.eventType());
                    }
                });
    }
}
