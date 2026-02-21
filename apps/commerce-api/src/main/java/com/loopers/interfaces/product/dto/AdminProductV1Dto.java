package com.loopers.interfaces.product.dto;

import com.loopers.application.product.dto.ProductCommand;
import com.loopers.application.product.dto.ProductInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.List;

public class AdminProductV1Dto {

    public record RegisterRequest(
        @NotNull(message = "브랜드 ID는 필수 입력값입니다.")
        Long brandId,
        @NotBlank(message = "상품명은 필수 입력값입니다.")
        @Size(max = 99, message = "상품명은 99자 이하여야 합니다.")
        String name,
        @NotNull(message = "가격은 필수 입력값입니다.")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Integer price,
        @NotNull(message = "재고는 필수 입력값입니다.")
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        Integer stock
    ) {
        public ProductCommand.Register toCommand() {
            return new ProductCommand.Register(brandId, name, price, stock);
        }
    }

    public record UpdateRequest(
        @NotBlank(message = "상품명은 필수 입력값입니다.")
        @Size(max = 99, message = "상품명은 99자 이하여야 합니다.")
        String name,
        @NotNull(message = "가격은 필수 입력값입니다.")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Integer price,
        @NotNull(message = "재고는 필수 입력값입니다.")
        @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
        Integer stock
    ) {
        public ProductCommand.Update toCommand() {
            return new ProductCommand.Update(name, price, stock);
        }
    }

    public record DetailResponse(
        Long id,
        Long brandId,
        String brandName,
        String name,
        int price,
        int stock,
        int likeCount,
        ZonedDateTime createdAt,
        ZonedDateTime updatedAt,
        ZonedDateTime deletedAt
    ) {
        public static DetailResponse from(ProductInfo info) {
            return new DetailResponse(
                info.id(), info.brandId(), info.brandName(),
                info.name(), info.price(), info.stock(), info.likeCount(),
                info.createdAt(), info.updatedAt(), info.deletedAt()
            );
        }
    }

    public record ListResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<ListItem> items
    ) {
        public record ListItem(
            Long id,
            Long brandId,
            String brandName,
            String name,
            int price,
            int stock,
            int likeCount,
            ZonedDateTime createdAt,
            ZonedDateTime updatedAt,
            ZonedDateTime deletedAt
        ) {
            public static ListItem from(ProductInfo info) {
                return new ListItem(
                    info.id(), info.brandId(), info.brandName(),
                    info.name(), info.price(), info.stock(), info.likeCount(),
                    info.createdAt(), info.updatedAt(), info.deletedAt()
                );
            }
        }
    }
}
