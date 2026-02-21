package com.loopers.interfaces.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.dto.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.product.dto.ProductV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<ProductV1Dto.ListResponse> list(
        @RequestParam(required = false) Long brandId,
        @RequestParam(defaultValue = "latest") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, toSort(sort));
        Page<ProductInfo> productInfoPage = brandId != null
            ? productFacade.getAllActiveByBrandId(brandId, pageRequest)
            : productFacade.getAllActive(pageRequest);

        ProductV1Dto.ListResponse listResponse = new ProductV1Dto.ListResponse(
            productInfoPage.getNumber(),
            productInfoPage.getSize(),
            productInfoPage.getTotalElements(),
            productInfoPage.getTotalPages(),
            productInfoPage.getContent().stream()
                .map(ProductV1Dto.ListResponse.ListItem::from)
                .toList()
        );
        return ApiResponse.success(listResponse);
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.DetailResponse> getById(
        @PathVariable Long productId
    ) {
        ProductInfo productInfo = productFacade.getById(productId);
        return ApiResponse.success(ProductV1Dto.DetailResponse.from(productInfo));
    }

    private Sort toSort(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price.value");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likeCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
