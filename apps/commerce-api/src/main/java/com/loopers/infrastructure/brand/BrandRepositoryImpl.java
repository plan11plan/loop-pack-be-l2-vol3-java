package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {
    private final BrandJpaRepository brandJpaRepository;

    @Override
    public BrandModel save(BrandModel brandModel) {
        return brandJpaRepository.save(brandModel);
    }

    @Override
    public Optional<BrandModel> findById(Long id) {
        return brandJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<BrandModel> findByName(String name) {
        return brandJpaRepository.findByNameAndDeletedAtIsNull(name);
    }
}
