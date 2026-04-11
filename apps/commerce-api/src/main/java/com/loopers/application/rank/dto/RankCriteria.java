package com.loopers.application.rank.dto;

import java.time.LocalDate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class RankCriteria {

    public record Search(LocalDate date, int page, int size) {

        public Pageable toPageable() {
            return PageRequest.of(page - 1, size);
        }
    }
}
