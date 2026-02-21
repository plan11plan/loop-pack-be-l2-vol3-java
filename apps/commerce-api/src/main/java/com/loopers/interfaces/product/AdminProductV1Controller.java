package com.loopers.interfaces.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.dto.ProductInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.product.dto.AdminProductV1Dto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/products")
public class AdminProductV1Controller implements AdminProductV1ApiSpec {

    private final ProductFacade productFacade;

    @PostMapping
    @Override
    public ApiResponse<Object> register(
        @Valid @RequestBody AdminProductV1Dto.RegisterRequest request
    ) {
        productFacade.register(request.toCommand());
        return ApiResponse.success();
    }

    @GetMapping
    @Override
    public ApiResponse<AdminProductV1Dto.ListResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Long brandId
    ) {
        Page<ProductInfo> productInfoPage = brandId != null
            ? productFacade.getAllByBrandId(brandId, PageRequest.of(page, size))
            : productFacade.getAll(PageRequest.of(page, size));

        AdminProductV1Dto.ListResponse listResponse = new AdminProductV1Dto.ListResponse(
            productInfoPage.getNumber(),
            productInfoPage.getSize(),
            productInfoPage.getTotalElements(),
            productInfoPage.getTotalPages(),
            productInfoPage.getContent().stream()
                .map(AdminProductV1Dto.ListResponse.ListItem::from)
                .toList()
        );
        return ApiResponse.success(listResponse);
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<AdminProductV1Dto.DetailResponse> getById(
        @PathVariable Long productId
    ) {
        ProductInfo productInfo = productFacade.getById(productId);
        return ApiResponse.success(AdminProductV1Dto.DetailResponse.from(productInfo));
    }

    @PutMapping("/{productId}")
    @Override
    public ApiResponse<Object> update(
        @PathVariable Long productId,
        @Valid @RequestBody AdminProductV1Dto.UpdateRequest request
    ) {
        productFacade.update(productId, request.toCommand());
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    @Override
    public ApiResponse<Object> delete(
        @PathVariable Long productId
    ) {
        productFacade.delete(productId);
        return ApiResponse.success();
    }
}
