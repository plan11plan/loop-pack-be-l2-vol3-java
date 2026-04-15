package com.loopers.application.metrics;

import com.loopers.domain.event.EventHandledEntity;
import com.loopers.infrastructure.event.EventHandledJpaRepository;
import com.loopers.infrastructure.metrics.ProductMetricsHourlyJpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsAggregationService {

    private final ProductMetricsHourlyJpaRepository hourlyRepository;
    private final EventHandledJpaRepository eventHandledRepository;

    @Transactional
    public void incrementViewCount(String eventId, Long productId) {
        if (isAlreadyHandled(eventId)) return;

        hourlyRepository.upsertMetrics(
                productId, LocalDate.now(), LocalTime.now().getHour(), 1, 0, 0);
        markHandled(eventId);
        log.info("[Metrics] 조회수 +1 — productId={}", productId);
    }

    @Transactional
    public void addLikeCount(String eventId, Long productId, long delta) {
        if (isAlreadyHandled(eventId)) return;

        hourlyRepository.upsertMetrics(
                productId, LocalDate.now(), LocalTime.now().getHour(), 0, delta, 0);
        markHandled(eventId);
        log.info("[Metrics] 좋아요 {} — productId={}", delta > 0 ? "+" + delta : delta, productId);
    }

    @Transactional
    public void addSalesCount(String eventId, List<Long> productIds) {
        if (isAlreadyHandled(eventId)) return;

        LocalDate today = LocalDate.now();
        int hour = LocalTime.now().getHour();
        for (Long productId : productIds) {
            hourlyRepository.upsertMetrics(productId, today, hour, 0, 0, 1);
        }
        markHandled(eventId);
        log.info("[Metrics] 주문 완료 집계 — {}개 상품", productIds.size());
    }

    private boolean isAlreadyHandled(String eventId) {
        if (eventHandledRepository.existsById(eventId)) {
            log.info("[Metrics] 이미 처리된 이벤트 스킵 — eventId={}", eventId);
            return true;
        }
        return false;
    }

    private void markHandled(String eventId) {
        eventHandledRepository.save(new EventHandledEntity(eventId));
    }
}
