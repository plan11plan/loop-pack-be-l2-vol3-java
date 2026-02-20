package com.loopers.application.brand;

import com.loopers.application.brand.dto.BrandCommand;
import com.loopers.application.brand.dto.BrandInfo;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandFacade {

    private final BrandService brandService;

    @Transactional
    public void register(BrandCommand.Register command) {
        brandService.register(command.name());
    }

    @Transactional(readOnly = true)
    public BrandInfo getById(Long id) {
        BrandModel brandModel = brandService.getById(id);
        return BrandInfo.from(brandModel);
    }

    @Transactional
    public void update(Long id, BrandCommand.Update command) {
        brandService.update(id, command.name());
    }

    @Transactional
    public void delete(Long id) {
        brandService.delete(id);
    }

    @Transactional(readOnly = true)
    public Page<BrandInfo> getAll(Pageable pageable) {
        return brandService.getAll(pageable).map(BrandInfo::from);
    }
}
