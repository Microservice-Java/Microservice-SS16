package com.example.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * CustomCacheErrorHandler provides a graceful fallback mechanism when Redis is down or unavailable.
 * When Redis experiences a connection failure or timeout during GET, PUT, or EVICT,
 * this handler swallows the exception and logs a warning, allowing the application to continue with RDBMS.
 */
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Cache Error Fallback] Redis Read Error for key '{}' in cache '{}'. Falling back to Database read. Cause: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("[Cache Error Fallback] Redis Put Error for key '{}' in cache '{}'. Operations continue with DB. Cause: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Cache Error Fallback] Redis Evict Error for key '{}' in cache '{}'. Eviction failed! Short TTL will auto-expire stale entries. Cause: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("[Cache Error Fallback] Redis Clear Error for cache '{}'. Cause: {}",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }
}
