package com.example.product.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * CustomCacheErrorHandler provides a graceful fallback mechanism when Redis is down or unavailable.
 * Instead of crashing the application with a RedisConnectionFailureException or QueryTimeoutException,
 * this handler swallows the cache exception and logs a warning, allowing Spring Cache to fall back
 * seamlessly to querying the primary Database.
 */
@Slf4j
public class CustomCacheErrorHandler implements CacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Cache Error Fallback] Unable to retrieve key '{}' from cache '{}'. Falling back to database. Cause: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("[Cache Error Fallback] Unable to put key '{}' into cache '{}'. Operations continue with DB. Cause: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("[Cache Error Fallback] Unable to evict key '{}' from cache '{}'. Operations continue with DB. Cause: {}",
                key, cache != null ? cache.getName() : "unknown", exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("[Cache Error Fallback] Unable to clear cache '{}'. Operations continue with DB. Cause: {}",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }
}
