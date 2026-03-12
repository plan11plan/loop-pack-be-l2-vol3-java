package com.loopers.support.cache;

import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {

    BRAND_LIST(Names.BRAND_LIST, Duration.ofHours(24)),
    PRODUCT_DETAIL(Names.PRODUCT_DETAIL, Duration.ofMinutes(10));

    private final String cacheName;
    private final Duration ttl;

    public static class Names {
        public static final String BRAND_LIST = "brand:list";
        public static final String PRODUCT_DETAIL = "product:detail";
    }
}
