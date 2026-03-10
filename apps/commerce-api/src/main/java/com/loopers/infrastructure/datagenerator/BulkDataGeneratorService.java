package com.loopers.infrastructure.datagenerator;

import com.loopers.domain.user.PasswordEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkDataGeneratorService {

    private final DataGeneratorRepository dataGeneratorRepository;
    private final PasswordEncoder passwordEncoder;
    private final BulkDataGeneratorProperties properties;

    private volatile boolean running = false;

    public boolean isRunning() {
        return running;
    }

    public void generateAll() {
        if (running) {
            log.warn("BulkDataGenerator is already running. Skipping.");
            return;
        }
        running = true;
        try {
            log.info("=== BulkDataGenerator START === brands={}, products={}, users={}, likes={}, orders={}",
                    properties.brandCount(), properties.productCount(),
                    properties.userCount(), properties.likeCount(), properties.orderCount());

            long totalStart = System.currentTimeMillis();
            Map<String, Long> stats = dataGeneratorRepository.getStats();

            List<Long> brandIds;
            if (stats.get("brandCount") < properties.brandCount()) {
                brandIds = generateBrands();
            } else {
                brandIds = dataGeneratorRepository.findAllBrandIds();
                log.info("  Phase 1: Brands {} already exist. Skipping.", brandIds.size());
            }

            if (stats.get("productCount") < properties.productCount()) {
                generateProducts(brandIds);
            } else {
                log.info("  Phase 2: Products {} already exist. Skipping.", stats.get("productCount"));
            }

            if (stats.get("userCount") < properties.userCount()) {
                generateUsers();
            } else {
                log.info("  Phase 3: Users {} already exist. Skipping.", stats.get("userCount"));
            }

            if (stats.get("likeCount") < properties.likeCount()) {
                generateLikes();
            } else {
                log.info("  Phase 4: Likes {} already exist. Skipping.", stats.get("likeCount"));
            }

            if (stats.get("orderCount") < properties.orderCount()) {
                generateOrders();
            } else {
                log.info("  Phase 5: Orders {} already exist. Skipping.", stats.get("orderCount"));
            }

            Map<String, Long> finalStats = dataGeneratorRepository.getStats();
            log.info("=== BulkDataGenerator DONE === elapsed={}s | brands={}, products={}, users={}, likes={}, orders={}",
                    elapsed(totalStart),
                    finalStats.get("brandCount"), finalStats.get("productCount"),
                    finalStats.get("userCount"), finalStats.get("likeCount"), finalStats.get("orderCount"));
        } finally {
            running = false;
        }
    }

    private List<Long> generateBrands() {
        long start = System.currentTimeMillis();
        List<String> brandNames = new ArrayList<>();
        for (int i = 1; i <= properties.brandCount(); i++) {
            brandNames.add(String.format("Brand_%03d", i));
        }
        dataGeneratorRepository.batchInsertBrands(brandNames);
        List<Long> brandIds = dataGeneratorRepository.findAllBrandIds();
        log.info("  Phase 1: Brands {} created ({}s)", brandIds.size(), elapsed(start));
        return brandIds;
    }

    private void generateProducts(List<Long> brandIds) {
        if (brandIds.isEmpty()) {
            log.warn("  Phase 2: No brands available. Skipping product generation.");
            return;
        }

        long start = System.currentTimeMillis();
        Random random = new Random(42);
        int totalProducts = properties.productCount();
        int batchSize = 10_000;

        List<Object[]> batch = new ArrayList<>(batchSize);
        int created = 0;

        for (int i = 0; i < totalProducts; i++) {
            int brandIndex = (int) (Math.pow(random.nextDouble(), 1.5) * brandIds.size());
            Long brandId = brandIds.get(brandIndex);

            int price = ((int) (1000 + Math.pow(random.nextDouble(), 0.7) * 499_000) / 100) * 100;
            int stock = random.nextDouble() < 0.05 ? 0 : random.nextInt(1001);
            String name = "P_" + brandIndex + "_" + (i + 1);

            batch.add(new Object[]{brandId, name, price, stock});

            if (batch.size() >= batchSize) {
                dataGeneratorRepository.batchInsertProducts(batch);
                created += batch.size();
                batch.clear();
                log.info("  Phase 2: Products {}/{} ({}s)", created, totalProducts, elapsed(start));
            }
        }

        if (!batch.isEmpty()) {
            dataGeneratorRepository.batchInsertProducts(batch);
            created += batch.size();
            batch.clear();
        }

        log.info("  Phase 2: Products {} created ({}s)", created, elapsed(start));
    }

    private void generateUsers() {
        long start = System.currentTimeMillis();
        String encodedPassword = passwordEncoder.encode("Test1234!");
        int created = dataGeneratorRepository.batchInsertUsers(
                "bulk", properties.userCount(), encodedPassword, 1_000_000L);
        log.info("  Phase 3: Users {} created ({}s)", created, elapsed(start));
    }

    private void generateLikes() {
        long start = System.currentTimeMillis();
        List<Long> productIds = dataGeneratorRepository.findAllProductIds();
        List<Long> userIds = dataGeneratorRepository.findAllUserIds();

        if (productIds.isEmpty() || userIds.isEmpty()) {
            log.warn("  Phase 4: No products or users. Skipping like generation.");
            return;
        }

        int totalLikes = properties.likeCount();

        // Zipf distribution (exponent 1.2): 상위 1% 상품이 ~70% 좋아요 차지
        double[] weights = new double[productIds.size()];
        double sumWeights = 0;
        for (int i = 0; i < productIds.size(); i++) {
            weights[i] = 1.0 / Math.pow(i + 1, 1.2);
            sumWeights += weights[i];
        }

        int[] likesPerProduct = new int[productIds.size()];
        int assigned = 0;
        for (int i = 0; i < productIds.size() && assigned < totalLikes; i++) {
            likesPerProduct[i] = Math.min(
                    (int) (weights[i] / sumWeights * totalLikes),
                    userIds.size());
            assigned += likesPerProduct[i];
        }
        for (int i = 0; assigned < totalLikes && i < productIds.size(); i++) {
            if (likesPerProduct[i] < userIds.size()) {
                likesPerProduct[i]++;
                assigned++;
            }
        }

        // Generate pairs in chunks
        Random random = new Random(42);
        int chunkSize = 50_000;
        List<long[]> chunk = new ArrayList<>(chunkSize);
        int totalCreated = 0;

        for (int i = 0; i < productIds.size(); i++) {
            int count = likesPerProduct[i];
            if (count == 0) continue;

            long productId = productIds.get(i);
            Set<Integer> selectedUserIndices = new HashSet<>();
            int attempts = 0;
            while (selectedUserIndices.size() < count && attempts < count * 3) {
                selectedUserIndices.add(random.nextInt(userIds.size()));
                attempts++;
            }

            for (int userIndex : selectedUserIndices) {
                chunk.add(new long[]{userIds.get(userIndex), productId});

                if (chunk.size() >= chunkSize) {
                    dataGeneratorRepository.batchInsertLikes(chunk);
                    totalCreated += chunk.size();
                    chunk.clear();
                    log.info("  Phase 4: Likes {}/{} ({}s)", totalCreated, totalLikes, elapsed(start));
                }
            }
        }

        if (!chunk.isEmpty()) {
            dataGeneratorRepository.batchInsertLikes(chunk);
            totalCreated += chunk.size();
            chunk.clear();
        }

        log.info("  Phase 4: Likes {} created ({}s)", totalCreated, elapsed(start));
    }

    private void generateOrders() {
        long start = System.currentTimeMillis();
        List<Long> userIds = dataGeneratorRepository.findAllUserIds();
        // 좋아요 순 정렬된 상품 풀 (인덱스 0 = 가장 인기 많은 상품)
        List<Map<String, Object>> productPool = dataGeneratorRepository.findProductsForOrders(5000);

        if (userIds.isEmpty() || productPool.isEmpty()) {
            log.warn("  Phase 5: No users or products. Skipping order generation.");
            return;
        }

        Random random = new Random(42);
        int totalOrders = properties.orderCount();
        int ordersPerUser = Math.max(1, totalOrders / userIds.size());
        int chunkSize = 1000;
        int created = 0;
        int poolSize = productPool.size();

        List<Object[]> orderBatch = new ArrayList<>(chunkSize);
        List<List<Object[]>> itemsPerOrder = new ArrayList<>(chunkSize);

        for (Long userId : userIds) {
            if (created + orderBatch.size() >= totalOrders) break;

            for (int o = 0; o < ordersPerUser && created + orderBatch.size() < totalOrders; o++) {
                int itemCount = 1 + random.nextInt(3);
                int totalPrice = 0;
                List<Object[]> items = new ArrayList<>(itemCount);

                for (int i = 0; i < itemCount; i++) {
                    // Power-law: 인기 상품(낮은 인덱스)일수록 더 자주 선택
                    int productIndex = (int) (Math.pow(random.nextDouble(), 1.5) * poolSize);
                    Map<String, Object> product = productPool.get(productIndex);
                    int price = ((Number) product.get("price")).intValue();
                    int qty = 1 + random.nextInt(3);
                    totalPrice += price * qty;
                    items.add(new Object[]{
                            ((Number) product.get("id")).longValue(),
                            price, qty,
                            (String) product.get("product_name"),
                            (String) product.get("brand_name"),
                            "ORDERED"});
                }

                orderBatch.add(new Object[]{userId, totalPrice, totalPrice, "ORDERED", 0});
                itemsPerOrder.add(items);
            }

            if (orderBatch.size() >= chunkSize) {
                created += flushOrderBatch(orderBatch, itemsPerOrder);
                log.info("  Phase 5: Orders {}/{} ({}s)", created, totalOrders, elapsed(start));
            }
        }

        if (!orderBatch.isEmpty()) {
            created += flushOrderBatch(orderBatch, itemsPerOrder);
        }

        log.info("  Phase 5: Orders {} created ({}s)", created, elapsed(start));
    }

    private int flushOrderBatch(List<Object[]> orderBatch, List<List<Object[]>> itemsPerOrder) {
        long maxId = dataGeneratorRepository.getMaxOrderId();
        dataGeneratorRepository.batchInsertOrders(orderBatch);

        List<Object[]> allItems = new ArrayList<>();
        for (int i = 0; i < orderBatch.size(); i++) {
            long orderId = maxId + 1 + i;
            for (Object[] item : itemsPerOrder.get(i)) {
                allItems.add(new Object[]{
                        orderId, item[0], item[1], item[2], item[3], item[4], item[5]});
            }
        }
        dataGeneratorRepository.batchInsertOrderItems(allItems);

        int size = orderBatch.size();
        orderBatch.clear();
        itemsPerOrder.clear();
        return size;
    }

    private String elapsed(long startMs) {
        return String.format("%.1f", (System.currentTimeMillis() - startMs) / 1000.0);
    }
}
