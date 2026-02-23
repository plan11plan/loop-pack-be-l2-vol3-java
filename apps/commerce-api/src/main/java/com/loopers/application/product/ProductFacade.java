package com.loopers.application.product;

import com.loopers.application.product.dto.ProductCriteria;
import com.loopers.application.product.dto.ProductResult;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
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
        BrandModel brand = brandService.getById(criteria.brandId());
        productService.register(brand, criteria.name(), criteria.price(), criteria.stock());
    }

    @Transactional(readOnly = true)
    public ProductResult getProduct(Long id) {
        ProductModel productModel = productService.getById(id);
        return ProductResult.from(productModel);
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
        return productService.getAll(pageable).map(ProductResult::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsByBrandId(Long brandId, Pageable pageable) {
        return productService.getAllByBrandId(brandId, pageable).map(ProductResult::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getActiveProducts(Pageable pageable) {
        return productService.getAllActive(pageable).map(ProductResult::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getActiveProductsByBrandId(Long brandId, Pageable pageable) {
        return productService.getAllActiveByBrandId(brandId, pageable).map(ProductResult::from);
    }
}
