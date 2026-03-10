package com.loopers.infrastructure.datagenerator;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.data-generator")
public record BulkDataGeneratorProperties(
        boolean enabled,
        int brandCount,
        int productCount,
        int userCount,
        int likeCount,
        int orderCount) {

    public BulkDataGeneratorProperties {
        if (brandCount <= 0) brandCount = 100;
        if (productCount <= 0) productCount = 100_000;
        if (userCount <= 0) userCount = 10_000;
        if (likeCount <= 0) likeCount = 500_000;
        if (orderCount <= 0) orderCount = 100_000;
    }
}
