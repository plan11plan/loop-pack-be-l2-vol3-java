package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    public void register(Long brandId, String name, int price, int stock) {
        productRepository.save(ProductModel.create(brandId, name, price, stock));
    }

    @Transactional(readOnly = true)
    public ProductModel getById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new CoreException(ProductErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ProductModel> getAllByIds(List<Long> ids) {
        List<ProductModel> products = productRepository.findAllByIdIn(ids);
        if (products.size() != ids.size()) {
            throw new CoreException(ProductErrorCode.NOT_FOUND);
        }
        return products;
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
    public List<ProductModel> getAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<ProductModel> getAllByBrandId(Long brandId, Pageable pageable) {
        return productRepository.findAllByBrandId(brandId, pageable);
    }

    @Transactional
    public void deleteAllByBrandId(Long brandId) {
        List<ProductModel> products = productRepository.findAllByBrandId(brandId);
        products.forEach(ProductModel::delete);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return productRepository.findById(id).isPresent();
    }

    @Transactional(readOnly = true)
    public void validateExists(Long id) {
        if (!existsById(id)) {
            throw new CoreException(ProductErrorCode.NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public List<ProductModel> getAllByBrandId(Long brandId) {
        return productRepository.findAllByBrandId(brandId);
    }

    @Transactional
    public List<ProductSnapshot> validateAndDeductStock(List<StockDeductionCommand> commands) {
        List<ProductModel> products = getAllByIds(
                commands.stream().map(StockDeductionCommand::productId).toList());

        Map<Long, ProductModel> productMap = products.stream()
                .collect(Collectors.toMap(ProductModel::getId, Function.identity()));

        return commands.stream()
                .map(command -> {
                    ProductModel product = productMap.get(command.productId());
                    product.validateExpectedPrice(command.expectedPrice());
                    product.decreaseStock(command.quantity());
                    return new ProductSnapshot(
                            command.productId(),
                            product.getName(),
                            product.getPrice(),
                            command.quantity(),
                            product.getBrandId());
                })
                .toList();
    }
}
