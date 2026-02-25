package com.loopers.application.product;

import com.loopers.application.product.dto.ProductCriteria;
import com.loopers.application.product.dto.ProductResult;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;

    @Transactional
    public void registerProduct(ProductCriteria.Register criteria) {
        brandService.validateExists(criteria.brandId());
        productService.register(criteria.brandId(), criteria.name(), criteria.price(), criteria.stock());
    }

    @Transactional(readOnly = true)
    public ProductResult getProduct(Long id) {
        ProductModel product = productService.getById(id);
        BrandModel brand = brandService.getById(product.getBrandId());
        return ProductResult.of(product, brand.getName());
    }

    @Transactional
    public void updateProduct(Long id, ProductCriteria.Update criteria) {
        productService.update(id, criteria.name(), criteria.price(), criteria.stock());
    }

    @Transactional
    public void deleteProduct(Long id) {
        productService.delete(id);
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProducts(Pageable pageable) {
        Page<ProductModel> products = productService.getAll(pageable);
        Map<Long, String> brandNameMap = getBrandNameMap(products.getContent());
        return products.map(product -> ProductResult.of(product, brandNameMap.get(product.getBrandId())));
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsByBrandId(Long brandId, Pageable pageable) {
        BrandModel brand = brandService.getById(brandId);
        return productService.getAllByBrandId(brandId, pageable)
                .map(product -> ProductResult.of(product, brand.getName()));
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsWithActiveBrand(Pageable pageable) {
        Page<ProductModel> products = productService.getAll(pageable);
        Map<Long, String> brandNameMap = getActiveBrandNameMap(products.getContent());
        return products.map(product -> ProductResult.of(product, brandNameMap.get(product.getBrandId())));
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsWithActiveBrandByBrandId(Long brandId, Pageable pageable) {
        BrandModel brand = brandService.getById(brandId);
        return productService.getAllByBrandId(brandId, pageable)
                .map(product -> ProductResult.of(product, brand.getName()));
    }

    private Map<Long, String> getBrandNameMap(List<ProductModel> products) {
        List<Long> brandIds = products.stream()
                .map(ProductModel::getBrandId)
                .distinct()
                .toList();
        return brandService.getAllByIds(brandIds).stream()
                .collect(Collectors.toMap(BrandModel::getId, BrandModel::getName));
    }

    private Map<Long, String> getActiveBrandNameMap(List<ProductModel> products) {
        List<Long> brandIds = products.stream()
                .map(ProductModel::getBrandId)
                .distinct()
                .toList();
        return brandService.getAllByIds(brandIds).stream()
                .filter(brand -> brand.getDeletedAt() == null)
                .collect(Collectors.toMap(BrandModel::getId, BrandModel::getName));
    }
}
