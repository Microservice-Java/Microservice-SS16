package com.example.product.service;

import com.example.product.config.CustomCacheErrorHandler;
import com.example.product.config.RedisConfig;
import com.example.product.dto.ProductDTO;
import com.example.product.dto.UpdateProductRequestDTO;
import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
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
class ProductServiceTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager(RedisConfig.PRODUCTS_CACHE);
        }
    }

    @Autowired
    private ProductService productService;

    @MockitoBean
    private ProductRepository productRepository;

    @Autowired
    private CacheManager cacheManager;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        try {
            Cache cache = cacheManager.getCache(RedisConfig.PRODUCTS_CACHE);
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            // Ignore cache clear error during test setup
        }

        sampleProduct = Product.builder()
                .id("P001")
                .name("MacBook Pro M3")
                .price(45000000.0)
                .stockQuantity(50)
                .description("Apple MacBook Pro 14-inch M3 Chip")
                .build();

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("1. Verify @CacheEvict Strategy for 100:1 Read:Write System (Cache Miss -> Hit -> Evict -> Miss)")
    void testCacheEvictStrategy_ReadWriteReadFlow() {
        when(productRepository.findById("P001")).thenReturn(Optional.of(sampleProduct));

        // 1st Read (Cache Miss): Queries Database and populates Redis Cache
        ProductDTO dto1 = productService.getProductById("P001");
        assertNotNull(dto1);
        assertEquals(45000000.0, dto1.getPrice());
        verify(productRepository, times(1)).findById("P001");

        // 2nd Read (Cache Hit): Reads directly from Cache without calling DB
        ProductDTO dto2 = productService.getProductById("P001");
        assertNotNull(dto2);
        assertEquals(45000000.0, dto2.getPrice());
        verify(productRepository, times(1)).findById("P001"); // Invocation count remains 1!

        // Update Operation (Write): Admin updates price to 42,000,000đ -> Updates DB & Evicts Cache
        UpdateProductRequestDTO updateReq = UpdateProductRequestDTO.builder()
                .price(42000000.0)
                .build();

        ProductDTO updatedDto = productService.updateProduct("P001", updateReq);
        assertNotNull(updatedDto);
        assertEquals(42000000.0, updatedDto.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));

        // 3rd Read (Post-Evict Cache Miss): Must query DB again to get fresh updated price (42,000,000đ)
        sampleProduct.setPrice(42000000.0);
        when(productRepository.findById("P001")).thenReturn(Optional.of(sampleProduct));

        ProductDTO dto3 = productService.getProductById("P001");
        assertNotNull(dto3);
        assertEquals(42000000.0, dto3.getPrice());
        verify(productRepository, times(3)).findById("P001"); // Total 3 findById invocations (read 1, update, read 3)
    }

    @Test
    @DisplayName("2. Input Validation: Reject Negative Price or Stock Quantity")
    void testInputValidation_NegativePriceOrStock() {
        UpdateProductRequestDTO invalidPriceReq = UpdateProductRequestDTO.builder().price(-500.0).build();
        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct("P001", invalidPriceReq));

        UpdateProductRequestDTO invalidStockReq = UpdateProductRequestDTO.builder().stockQuantity(-10).build();
        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct("P001", invalidStockReq));

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Input Validation: Fail-Fast on Null or Blank Product ID")
    void testInputValidation_NullOrBlankProductId() {
        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(null));
        assertThrows(IllegalArgumentException.class, () -> productService.getProductById("   "));
        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(null, new UpdateProductRequestDTO()));
        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct("   ", new UpdateProductRequestDTO()));

        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("4. CustomCacheErrorHandler: Fallback gracefully when Redis is down")
    void testCustomCacheErrorHandler_FallbackOnRedisDown() {
        CustomCacheErrorHandler errorHandler = new CustomCacheErrorHandler();
        Cache mockCache = mock(Cache.class);
        when(mockCache.getName()).thenReturn("products");

        RedisConnectionFailureException redisException = new RedisConnectionFailureException("Redis Connection Refused");

        assertDoesNotThrow(() -> errorHandler.handleCacheGetError(redisException, mockCache, "P001"));
        assertDoesNotThrow(() -> errorHandler.handleCachePutError(redisException, mockCache, "P001", sampleProduct));
        assertDoesNotThrow(() -> errorHandler.handleCacheEvictError(redisException, mockCache, "P001"));
    }
}
