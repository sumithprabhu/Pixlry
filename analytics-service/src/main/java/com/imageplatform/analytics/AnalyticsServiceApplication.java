package com.imageplatform.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// @EnableCaching activates Spring's proxy-based caching infrastructure.
// Without this, @Cacheable / @CacheEvict annotations are silently ignored.
@SpringBootApplication
@EnableCaching
public class AnalyticsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
