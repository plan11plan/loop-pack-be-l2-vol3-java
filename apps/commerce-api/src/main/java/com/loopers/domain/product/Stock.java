package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;

@Embeddable
@EqualsAndHashCode
public class Stock {

    private int value;

    protected Stock() {}

    public Stock(int value) {
        validate(value);
        this.value = value;
    }

    private void validate(int value) {
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.");
        }
    }

    public void deduct(int quantity) {
        if (!hasEnough(quantity)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.value -= quantity;
    }

    public boolean hasEnough(int quantity) {
        return this.value >= quantity;
    }

    public int getValue() {
        return value;
    }
}
