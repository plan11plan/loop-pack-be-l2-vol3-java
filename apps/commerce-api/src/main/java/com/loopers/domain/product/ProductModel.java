package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "price", nullable = false))
    private Money price;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "stock", nullable = false))
    private Stock stock;

    @Column(name = "like_count", nullable = false)
    @ColumnDefault("0")
    private int likeCount = 0;

    // === 생성 === //

    private ProductModel(BrandModel brand, String name, Money price, Stock stock) {
        this.brand = brand;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public static ProductModel create(BrandModel brand, String name, int price, int stock) {
        validateBrand(brand);
        validateName(name);
        return new ProductModel(brand, name, new Money(price), new Stock(stock));
    }

    // === 도메인 로직 === //

    public void update(String name, int price, int stock) {
        validateName(name);
        this.name = name;
        this.price = new Money(price);
        this.stock = new Stock(stock);
    }

    public void decreaseStock(int quantity) {
        this.stock.deduct(quantity);
    }

    public boolean isSoldOut() {
        return this.stock.getValue() == 0;
    }

    public void addLikeCount() {
        this.likeCount++;
    }

    public void subtractLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
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
}
