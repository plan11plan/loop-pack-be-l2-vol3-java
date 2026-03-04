package com.loopers.application.product;

import com.loopers.application.product.dto.ProductCriteria;
import com.loopers.application.product.dto.ProductResult;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final ProductLikeService productLikeService;

    @Transactional
    public void registerProduct(ProductCriteria.Register criteria) {
        brandService.validateExists(criteria.brandId());
        productService.register(criteria.brandId(), criteria.name(), criteria.price(), criteria.stock());
    }

    @Transactional(readOnly = true)
    public ProductResult getProduct(Long id) {
        ProductModel product = productService.getById(id);
        return ProductResult.of(
                product,
                brandService.getById(product.getBrandId()).getName(),
                productLikeService.countLikes(id));
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
        Map<Long, String> brandNameMap = brandService.getNameMapByIds(
                ProductModel.extractDistinctBrandIds(products.getContent()));
        return products.map(product -> ProductResult.of(product, brandNameMap.get(product.getBrandId())));
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsByBrandId(Long brandId, Pageable pageable) {
        String brandName = brandService.getById(brandId).getName();
        return productService.getAllByBrandId(brandId, pageable)
                .map(product -> ProductResult.of(product, brandName));
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsWithActiveBrand(Pageable pageable) {
        Page<ProductModel> products = productService.getAll(pageable);
        Map<Long, String> brandNameMap = brandService.getActiveNameMapByIds(
                ProductModel.extractDistinctBrandIds(products.getContent()));
        Map<Long, Long> likeCountMap = productLikeService.countLikesByProductIds(
                ProductModel.extractIds(products.getContent()));
        return new PageImpl<>(
                ProductResult.fromWithActiveBrand(products.getContent(), brandNameMap, likeCountMap),
                products.getPageable(),
                products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsWithActiveBrandByBrandId(Long brandId, Pageable pageable) {
        Page<ProductModel> products = productService.getAllByBrandId(brandId, pageable);
        Map<Long, String> brandNameMap = brandService.getActiveNameMapByIds(List.of(brandId));
        Map<Long, Long> likeCountMap = productLikeService.countLikesByProductIds(
                ProductModel.extractIds(products.getContent()));
        return new PageImpl<>(
                ProductResult.fromWithActiveBrand(products.getContent(), brandNameMap, likeCountMap),
                products.getPageable(),
                products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ProductResult> getProductsWithActiveBrandSortedByLikes(Long brandId, int page, int size) {
        List<ProductModel> products = brandId != null
                ? productService.getAllByBrandId(brandId)
                : productService.getAll();
        Map<Long, String> brandNameMap = brandService.getActiveNameMapByIds(
                ProductModel.extractDistinctBrandIds(products));
        Map<Long, Long> likeCountMap = productLikeService.countLikesByProductIds(
                ProductModel.extractIds(products));
        List<ProductResult> sorted =
                ProductResult.fromWithActiveBrand(products, brandNameMap, likeCountMap).stream()
                        .sorted(Comparator.comparingLong(ProductResult::likeCount).reversed())
                        .toList();
        int start = page * size;
        return new PageImpl<>(
                start >= sorted.size()
                        ? List.of()
                        : sorted.subList(start, Math.min(start + size, sorted.size())),
                PageRequest.of(page, size),
                sorted.size());
    }
}
