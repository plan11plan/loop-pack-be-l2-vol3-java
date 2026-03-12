package com.loopers.interfaces.brand;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.dto.BrandResult;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.brand.dto.AdminBrandV1Dto;
import com.loopers.support.cache.RedisCacheHelper;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api-admin/v1/brands")
public class AdminBrandV1Controller implements AdminBrandV1ApiSpec {

    private static final String BRAND_LIST_CACHE_KEY = "brand:list:all";
    private static final Duration BRAND_LIST_TTL = Duration.ofHours(24);

    private final BrandFacade brandFacade;
    private final RedisCacheHelper redisCacheHelper;

    @PostMapping
    @Override
    public ApiResponse<Object> register(
        @Valid @RequestBody AdminBrandV1Dto.RegisterRequest request
    ) {
        brandFacade.registerBrand(request.toCriteria());
        redisCacheHelper.delete(BRAND_LIST_CACHE_KEY);
        return ApiResponse.success();
    }

    @GetMapping
    @Override
    public ApiResponse<AdminBrandV1Dto.ListResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Optional<AdminBrandV1Dto.ListResponse> cached =
                redisCacheHelper.get(BRAND_LIST_CACHE_KEY, new TypeReference<>() {});
        if (cached.isPresent()) {
            return ApiResponse.success(cached.get());
        }

        Page<BrandResult> brandInfoPage = brandFacade.getBrands(PageRequest.of(page, size));
        AdminBrandV1Dto.ListResponse response =
                new AdminBrandV1Dto.ListResponse(
                        brandInfoPage.getNumber(),
                        brandInfoPage.getSize(),
                        brandInfoPage.getTotalElements(),
                        brandInfoPage.getTotalPages(),
                        brandInfoPage.getContent().stream()
                                .map(AdminBrandV1Dto.ListResponse.ListItem::from)
                                .toList());

        redisCacheHelper.set(BRAND_LIST_CACHE_KEY, response, BRAND_LIST_TTL);
        return ApiResponse.success(response);
    }

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<AdminBrandV1Dto.DetailResponse> getById(
        @PathVariable Long brandId
    ) {
        return ApiResponse.success(
                AdminBrandV1Dto.DetailResponse.from(brandFacade.getBrand(brandId)));
    }

    @PutMapping("/{brandId}")
    @Override
    public ApiResponse<Object> update(
        @PathVariable Long brandId,
        @Valid @RequestBody AdminBrandV1Dto.UpdateRequest request
    ) {
        brandFacade.updateBrand(brandId, request.toCriteria());
        redisCacheHelper.delete(BRAND_LIST_CACHE_KEY);
        return ApiResponse.success();
    }

    @DeleteMapping("/{brandId}")
    @Override
    public ApiResponse<Object> delete(
        @PathVariable Long brandId
    ) {
        brandFacade.deleteBrand(brandId);
        redisCacheHelper.delete(BRAND_LIST_CACHE_KEY);
        return ApiResponse.success();
    }
}
