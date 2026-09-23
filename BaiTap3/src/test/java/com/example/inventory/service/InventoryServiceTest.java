package com.example.inventory.service;

import com.example.inventory.config.CustomCacheErrorHandler;
import com.example.inventory.config.RedisConfig;
import com.example.inventory.dto.ProductInventoryDTO;
import com.example.inventory.entity.ProductInventory;
import com.example.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class InventoryServiceTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager(RedisConfig.INVENTORY_CACHE);
        }
    }

    @Autowired
    private InventoryService inventoryService;

    @MockitoBean
    private InventoryRepository inventoryRepository;

    @Autowired
    private CacheManager cacheManager;

    private ProductInventory sampleInventory;

    @BeforeEach
    void setUp() {
        try {
            Cache cache = cacheManager.getCache(RedisConfig.INVENTORY_CACHE);
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            // Ignore cache clear error if cache is unavailable during test setup
        }

        sampleInventory = ProductInventory.builder()
                .productId("PROD_IPHONE15")
                .productName("iPhone 15 Pro Max 256GB")
                .quantity(100)
                .build();

        // Stub save to return the passed entity
        when(inventoryRepository.save(any(ProductInventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("1. Cache-Aside Pattern: Read (Cache Miss -> DB -> Cache Hit) & Write (DB Update -> Cache Evict)")
    void testCacheAsideReadWrite_FlowSuccess() {
        when(inventoryRepository.findById("PROD_IPHONE15")).thenReturn(Optional.of(sampleInventory));

        // 1st Read (Cache Miss): Queries Database and populates cache
        ProductInventoryDTO dto1 = inventoryService.getInventory("PROD_IPHONE15");
        assertNotNull(dto1);
        assertEquals(100, dto1.getQuantity());
        verify(inventoryRepository, times(1)).findById("PROD_IPHONE15");

        // 2nd Read (Cache Hit): Reads directly from Cache without calling DB
        ProductInventoryDTO dto2 = inventoryService.getInventory("PROD_IPHONE15");
        assertNotNull(dto2);
        assertEquals(100, dto2.getQuantity());
        verify(inventoryRepository, times(1)).findById("PROD_IPHONE15"); // Count remains 1!

        // Write Operation: Admin updates stock to 95 -> Updates DB and Evicts Cache
        ProductInventoryDTO updatedDto = inventoryService.updateInventory("PROD_IPHONE15", 95);
        assertNotNull(updatedDto);
        assertEquals(95, updatedDto.getQuantity());
        verify(inventoryRepository, times(1)).save(any(ProductInventory.class));

        // 3rd Read (Post Evict Cache Miss): Must query DB again for fresh quantity (95)
        sampleInventory.setQuantity(95);
        when(inventoryRepository.findById("PROD_IPHONE15")).thenReturn(Optional.of(sampleInventory));

        ProductInventoryDTO dto3 = inventoryService.getInventory("PROD_IPHONE15");
        assertNotNull(dto3);
        assertEquals(95, dto3.getQuantity());
        verify(inventoryRepository, times(3)).findById("PROD_IPHONE15"); // Total 3 invocations: 1st read, update lookup, 3rd read
    }

    @Test
    @DisplayName("2. Trap Scenario 1: Reject Negative Quantity (newQuantity < 0)")
    void testNegativeQuantityValidation_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                inventoryService.updateInventory("PROD_IPHONE15", -10));

        assertEquals("Inventory quantity cannot be negative", exception.getMessage());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Input Validation: Fail-Fast on Null or Blank Product ID")
    void testInputValidation_NullOrBlankProductId() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventory(null));
        assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventory("   "));
        assertThrows(IllegalArgumentException.class, () -> inventoryService.updateInventory(null, 50));
        assertThrows(IllegalArgumentException.class, () -> inventoryService.updateInventory("   ", 50));

        verifyNoInteractions(inventoryRepository);
    }

    @Test
    @DisplayName("4. Trap Scenario 2: Test CustomCacheErrorHandler Fallback when Redis Fails")
    void testCustomCacheErrorHandler_FallbackOnRedisDown() {
        CustomCacheErrorHandler errorHandler = new CustomCacheErrorHandler();
        Cache mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("inventory");

        RedisConnectionFailureException redisException = new RedisConnectionFailureException("Redis Connection Refused");

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(redisException, mockCache, "PROD_IPHONE15"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(redisException, mockCache, "PROD_IPHONE15", 95));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(redisException, mockCache, "PROD_IPHONE15"));
    }
}
