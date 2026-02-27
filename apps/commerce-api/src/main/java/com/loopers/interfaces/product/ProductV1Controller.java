package com.loopers.interfaces.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.dto.ProductResult;
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
        Page<ProductResult> productPage = "likes_desc".equals(sort)
                ? productFacade.getProductsWithActiveBrandSortedByLikes(brandId, page, size)
                : brandId != null
                        ? productFacade.getProductsWithActiveBrandByBrandId(
                                brandId, PageRequest.of(page, size, toSort(sort)))
                        : productFacade.getProductsWithActiveBrand(
                                PageRequest.of(page, size, toSort(sort)));

        return ApiResponse.success(
                new ProductV1Dto.ListResponse(
                        productPage.getNumber(),
                        productPage.getSize(),
                        productPage.getTotalElements(),
                        productPage.getTotalPages(),
                        productPage.getContent().stream()
                                .map(ProductV1Dto.ListResponse.ListItem::from)
                                .toList()));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.DetailResponse> getById(
        @PathVariable Long productId
    ) {
        ProductResult result = productFacade.getProduct(productId);
        return ApiResponse.success(ProductV1Dto.DetailResponse.from(result));
    }

    private Sort toSort(String sort) {
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price.value");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }
}
