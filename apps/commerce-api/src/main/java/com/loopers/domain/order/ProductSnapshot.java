package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProductSnapshot {

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "brand_name", nullable = false)
    private String brandName;

    private ProductSnapshot(String productName, String brandName) {
        validate(productName, brandName);
        this.productName = productName;
        this.brandName = brandName;
    }

    public static ProductSnapshot of(String productName, String brandName) {
        return new ProductSnapshot(productName, brandName);
    }

    private void validate(String productName, String brandName) {
        if (productName == null || productName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수값입니다.");
        }
        if (brandName == null || brandName.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드명은 필수값입니다.");
        }
    }
}
