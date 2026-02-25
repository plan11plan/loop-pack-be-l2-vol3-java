package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductModel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false, foreignKey = @ForeignKey(value = jakarta.persistence.ConstraintMode.NO_CONSTRAINT))
    private BrandModel brand;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "stock", nullable = false)
    private int stock;

    // === 생성 === //

    private ProductModel(BrandModel brand, String name, int price, int stock) {
        this.brand = brand;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public static ProductModel create(BrandModel brand, String name, int price, int stock) {
        validateBrand(brand);
        validateName(name);
        validatePriceRange(price);
        validateStockRange(stock);
        return new ProductModel(brand, name, price, stock);
    }

    // === 도메인 로직 === //

    public void update(String name, int price, int stock) {
        validateName(name);
        validatePriceRange(price);
        validateStockRange(stock);
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public void validateExpectedPrice(int expectedPrice) {
        if (expectedPrice != this.price) {
            throw new CoreException(ProductErrorCode.PRICE_MISMATCH);
        }
    }

    public void decreaseStock(int quantity) {
        if (quantity < 1) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.");
        }
        if (this.stock < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.stock -= quantity;
    }

    public boolean isSoldOut() {
        return this.stock == 0;
    }

    // === 검증 === //

    private static void validateBrand(BrandModel brand) {
        if (brand == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드는 필수값입니다.");
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수값입니다.");
        }
        if (name.length() > 99) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 99자 이하여야 합니다.");
        }
    }

    private static void validatePriceRange(int price) {
        if (price < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.");
        }
    }

    private static void validateStockRange(int stock) {
        if (stock < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.");
        }
    }
}
