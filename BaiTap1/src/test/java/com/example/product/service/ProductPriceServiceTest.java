package com.example.product.service;

import com.example.product.config.CustomCacheErrorHandler;
import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductPriceServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductPriceService productPriceService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id("P001")
                .name("Flash Sale Smartphone")
                .price(100000)
                .build();
    }

    @Test
    @DisplayName("1. Simulate Local Cache HashMap Inconsistency across Multi-Instances")
    void testLocalCacheInconsistency_MultiInstanceProblem() {
        // Shared Mock DB State
        Map<String, Integer> database = new HashMap<>();
        database.put("P001", 100000); // T0 Initial Price

        // Instance 1 Local Cache & Instance 2 Local Cache
        Map<String, Integer> instance1Cache = new HashMap<>();
        Map<String, Integer> instance2Cache = new HashMap<>();

        // T1: Customer 1 reads P001 from Inst1, Customer 2 reads P001 from Inst2
        instance1Cache.put("P001", database.get("P001"));
        instance2Cache.put("P001", database.get("P001"));

        assertEquals(100000, instance1Cache.get("P001"));
        assertEquals(100000, instance2Cache.get("P001"));

        // T2: Flash Sale Admin updates P001 price to 80,000đ via Instance 1
        database.put("P001", 80000);
        instance1Cache.put("P001", 80000); // Inst1 updates DB and local cache

        // T3: Inconsistency Verification!
        // Inst1 returns 80,000đ but Inst2 still returns stale 100,000đ from its independent local HashMap!
        assertEquals(80000, instance1Cache.get("P001"));
        assertEquals(100000, instance2Cache.get("P001"), "Instance 2 local cache is stale and inconsistent!");
        assertNotEquals(instance1Cache.get("P001"), instance2Cache.get("P001"));
    }

    @Test
    @DisplayName("2. Get Product Price - DB Query when not in cache")
    void testGetProductPrice_Success() {
        when(productRepository.findById("P001")).thenReturn(Optional.of(sampleProduct));

        Integer price = productPriceService.getProductPrice("P001");

        assertNotNull(price);
        assertEquals(100000, price);
        verify(productRepository, times(1)).findById("P001");
    }

    @Test
    @DisplayName("3. Update Product Price - Validates and saves to DB")
    void testUpdateProductPrice_Success() {
        when(productRepository.findById("P001")).thenReturn(Optional.of(sampleProduct));

        productPriceService.updateProductPrice("P001", 80000);

        assertEquals(80000, sampleProduct.getPrice());
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("4. Input Validation - Fail Fast on Null or Blank Product ID")
    void testInputValidation_NullOrBlankProductId() {
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
                productPriceService.getProductPrice(null));
        assertTrue(exNull.getMessage().contains("Product ID must not be null or blank"));

        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () ->
                productPriceService.getProductPrice("   "));
        assertTrue(exBlank.getMessage().contains("Product ID must not be null or blank"));

        IllegalArgumentException exUpdateNull = assertThrows(IllegalArgumentException.class, () ->
                productPriceService.updateProductPrice(null, 50000));
        assertTrue(exUpdateNull.getMessage().contains("Product ID must not be null or blank"));

        IllegalArgumentException exNegativePrice = assertThrows(IllegalArgumentException.class, () ->
                productPriceService.updateProductPrice("P001", -100));
        assertTrue(exNegativePrice.getMessage().contains("New price must be non-negative"));

        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("5. Test CustomCacheErrorHandler Fallback Behavior when Redis throws Connection Exception")
    void testCustomCacheErrorHandler_FallbackOnRedisDown() {
        CustomCacheErrorHandler errorHandler = new CustomCacheErrorHandler();
        Cache mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("product_prices");

        RedisConnectionFailureException redisException = new RedisConnectionFailureException("Cannot connect to Redis server");

        // Verify that errorHandler logs warning gracefully and does NOT throw exception out
        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(redisException, mockCache, "P001"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(redisException, mockCache, "P001", 80000));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(redisException, mockCache, "P001"));
        assertDoesNotThrow(() -> errorHandler.handleCacheClearError(redisException, mockCache));
    }
}
