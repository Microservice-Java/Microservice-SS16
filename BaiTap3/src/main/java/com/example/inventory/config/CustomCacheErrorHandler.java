package com.example.inventory.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * CustomCacheErrorHandler provides a resilient fallback strategy for Cache-Aside Pattern.
 * When Redis experiences a connection failure or timeout during READ (get) or WRITE (put/evict),
 * this handler swallows the exception and logs a warning so the application seamlessly falls back to RDBMS.
 */
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Cache-Aside Fallback] Redis Read Failure for key '{}' in cache '{}'. Falling back to Database read. Error: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("[Cache-Aside Fallback] Redis Put Failure for key '{}' in cache '{}'. Operations continue with DB write. Error: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Cache-Aside Fallback] Redis Evict Failure for key '{}' in cache '{}'. Cache eviction failed! Short TTL will auto-expire stale entries. Error: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("[Cache-Aside Fallback] Redis Clear Failure for cache '{}'. Error: {}",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }
}
