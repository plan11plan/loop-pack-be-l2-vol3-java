package com.loopers.application.order.dto;

import java.time.ZonedDateTime;
import java.util.List;

public class OrderCriteria {

    public record Create(List<CreateItem> items) {

        public record CreateItem(Long productId, int quantity, int expectedPrice) {}
    }

    public record ListByDate(ZonedDateTime startAt, ZonedDateTime endAt) {}
}
