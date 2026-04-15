package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.metrics.MetricsAggregationService;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.confg.kafka.KafkaTopics;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final MetricsAggregationService metricsAggregationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS,
            groupId = "commerce-streamer-group",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void consume(List<ConsumerRecord<String, byte[]>> records, Acknowledgment ack) {
        for (ConsumerRecord<String, byte[]> record : records) {
            try {
                JsonNode node = objectMapper.readTree(record.value());
                String eventId = node.get("eventId").asText();
                String eventType = node.get("eventType").asText();

                if ("ORDER_COMPLETED".equals(eventType)) {
                    JsonNode data = node.get("data");
                    JsonNode productIdsNode = data.get("productIds");
                    if (productIdsNode == null || !productIdsNode.isArray()) {
                        log.warn("[OrderConsumer] productIds 누락 — eventId={}", eventId);
                        continue;
                    }
                    List<Long> productIds = new ArrayList<>();
                    productIdsNode.forEach(n -> productIds.add(n.asLong()));
                    metricsAggregationService.addSalesCount(eventId, productIds);
                } else {
                    log.warn("[OrderConsumer] 알 수 없는 eventType={}", eventType);
                }
            } catch (Exception e) {
                log.error("[OrderConsumer] 메시지 처리 실패 — offset={}", record.offset(), e);
            }
        }
        ack.acknowledge();
    }
}
