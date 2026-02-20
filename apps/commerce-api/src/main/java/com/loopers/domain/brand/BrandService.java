package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;

    @Transactional
    public void register(String name) {
        if (brandRepository.findByName(name).isPresent()) {
            throw new CoreException(BrandErrorCode.DUPLICATE_NAME);
        }

        BrandModel brandModel = BrandModel.create(name);
        brandRepository.save(brandModel);
    }

    @Transactional(readOnly = true)
    public BrandModel getById(Long id) {
        return brandRepository.findById(id)
            .orElseThrow(() -> new CoreException(BrandErrorCode.NOT_FOUND));
    }

    @Transactional
    public void update(Long id, String name) {
        BrandModel brandModel = getById(id);

        brandRepository.findByName(name)
            .filter(existing -> !existing.getId().equals(brandModel.getId()))
            .ifPresent(existing -> {
                throw new CoreException(BrandErrorCode.DUPLICATE_NAME);
            });

        brandModel.update(name);
    }

    @Transactional
    public void delete(Long id) {
        BrandModel brandModel = getById(id);
        brandModel.delete();
    }
}
