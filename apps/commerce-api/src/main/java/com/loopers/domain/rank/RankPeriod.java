package com.loopers.domain.rank;

import java.time.LocalDate;

public enum RankPeriod {
    DAILY, WEEKLY, MONTHLY;

    public String toPeriodKey(LocalDate date) {
        return date.toString();
    }

    public LocalDate toEndDate(LocalDate date) {
        return switch (this) {
            case DAILY -> date;
            case WEEKLY, MONTHLY -> date.minusDays(1L);
        };
    }
}
