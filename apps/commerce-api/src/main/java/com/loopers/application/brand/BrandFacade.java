package com.loopers.application.brand;

import com.loopers.application.brand.dto.BrandCommand;
import com.loopers.application.brand.dto.BrandInfo;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandFacade {

    private final BrandService brandService;
    private final ProductService productService;

    @Transactional
    public void registerBrand(BrandCommand.Register command) {
        brandService.register(command.name());
    }

    @Transactional(readOnly = true)
    public BrandInfo getBrand(Long id) {
        BrandModel brandModel = brandService.getById(id);
        return BrandInfo.from(brandModel);
    }

    @Transactional
    public void updateBrand(Long id, BrandCommand.Update command) {
        brandService.update(id, command.name());
    }

    @Transactional
    public void deleteBrand(Long id) {
        brandService.delete(id);
        productService.softDeleteByBrandId(id);
    }

    @Transactional(readOnly = true)
    public Page<BrandInfo> getBrands(Pageable pageable) {
        return brandService.getAll(pageable).map(BrandInfo::from);
    }
}
