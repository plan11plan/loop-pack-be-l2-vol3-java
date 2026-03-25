package com.loopers.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaEventMessage;
import com.loopers.confg.kafka.KafkaTopics;
import com.loopers.domain.product.event.ProductLikedEvent;
import com.loopers.domain.product.event.ProductUnlikedEvent;
import com.loopers.domain.product.event.ProductViewedEvent;
import com.loopers.infrastructure.outbox.OutboxEventEntity;
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogKafkaPublisher {

    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductLiked(ProductLikedEvent event) {
        saveOutbox("PRODUCT_LIKED",
                KafkaTopics.CATALOG_EVENTS,
                String.valueOf(event.productId()),
                Map.of("productId", event.productId(), "userId", event.userId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleProductUnliked(ProductUnlikedEvent event) {
        saveOutbox("PRODUCT_UNLIKED",
                KafkaTopics.CATALOG_EVENTS,
                String.valueOf(event.productId()),
                Map.of("productId", event.productId(), "userId", event.userId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductViewed(ProductViewedEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", event.productId());
        data.put("userId", event.userId());

        saveOutbox("PRODUCT_VIEWED",
                KafkaTopics.CATALOG_EVENTS,
                String.valueOf(event.productId()),
                data);
    }

    private void saveOutbox(String eventType, String topic,
                            String partitionKey, Map<String, Object> data) {
        KafkaEventMessage message = KafkaEventMessage.of(eventType, data);
        try {
            outboxRepository.save(new OutboxEventEntity(
                    message.eventId(), eventType, topic, partitionKey,
                    objectMapper.writeValueAsString(message)));
        } catch (JsonProcessingException e) {
            log.error("[Outbox] JSON 직렬화 실패 — eventType={}", eventType, e);
            throw new RuntimeException(e);
        }
    }
}
