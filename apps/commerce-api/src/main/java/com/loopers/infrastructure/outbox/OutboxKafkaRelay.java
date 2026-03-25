package com.loopers.infrastructure.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxKafkaRelay {

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void relay() {
        List<OutboxEventEntity> events =
                outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        if (events.isEmpty()) return;

        for (OutboxEventEntity event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .get();
                event.markPublished();
                log.info("[OutboxRelay] 발행 성공 — topic={}, eventType={}, eventId={}",
                        event.getTopic(), event.getEventType(), event.getEventId());
            } catch (Exception e) {
                log.error("[OutboxRelay] 발행 실패 — eventId={}, 다음 주기에 재시도",
                        event.getEventId(), e);
                break;
            }
        }
    }
}
