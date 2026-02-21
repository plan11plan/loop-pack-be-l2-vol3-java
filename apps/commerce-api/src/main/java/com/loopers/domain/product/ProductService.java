package com.loopers.domain.product;

import com.loopers.domain.brand.BrandModel;
import com.loopers.support.error.CoreException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional
    public void register(BrandModel brand, String name, int price, int stock) {
        ProductModel productModel = ProductModel.create(brand, name, price, stock);
        productRepository.save(productModel);
    }

    @Transactional(readOnly = true)
    public ProductModel getById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CoreException(ProductErrorCode.NOT_FOUND));
    }

    @Transactional
    public void update(Long id, String name, int price, int stock) {
        ProductModel productModel = getById(id);
        productModel.update(name, price, stock);
    }

    @Transactional
    public void delete(Long id) {
        ProductModel productModel = getById(id);
        productModel.delete();
    }

    @Transactional(readOnly = true)
    public Page<ProductModel> getAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductModel> getAllByBrandId(Long brandId, Pageable pageable) {
        return productRepository.findAllByBrandId(brandId, pageable);
    }

    @Transactional
    public void softDeleteByBrandId(Long brandId) {
        List<ProductModel> products = productRepository.findAllByBrandId(brandId);
        products.forEach(ProductModel::delete);
    }

    @Transactional(readOnly = true)
    public Page<ProductModel> getAllActive(Pageable pageable) {
        return productRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ProductModel> getAllActiveByBrandId(Long brandId, Pageable pageable) {
        return productRepository.findAllActiveByBrandId(brandId, pageable);
    }
}
