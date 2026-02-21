package com.loopers.application.product;

import com.loopers.application.product.dto.ProductCommand;
import com.loopers.application.product.dto.ProductInfo;
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
    public void register(ProductCommand.Register command) {
        BrandModel brand = brandService.getById(command.brandId());
        productService.register(brand, command.name(), command.price(), command.stock());
    }

    @Transactional(readOnly = true)
    public ProductInfo getById(Long id) {
        ProductModel productModel = productService.getById(id);
        return ProductInfo.from(productModel);
    }

    @Transactional
    public void update(Long id, ProductCommand.Update command) {
        productService.update(id, command.name(), command.price(), command.stock());
    }

    @Transactional
    public void delete(Long id) {
        productService.delete(id);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getAll(Pageable pageable) {
        return productService.getAll(pageable).map(ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getAllByBrandId(Long brandId, Pageable pageable) {
        return productService.getAllByBrandId(brandId, pageable).map(ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getAllActive(Pageable pageable) {
        return productService.getAllActive(pageable).map(ProductInfo::from);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getAllActiveByBrandId(Long brandId, Pageable pageable) {
        return productService.getAllActiveByBrandId(brandId, pageable).map(ProductInfo::from);
    }
}
