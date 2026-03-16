package com.loopers.domain.payment;

public enum PgCallbackStatus {

    APPROVED,
    LIMIT_EXCEEDED,
    INVALID_CARD,
    PG_ERROR;

    public boolean isSuccess() {
        return this == APPROVED;
    }
}
