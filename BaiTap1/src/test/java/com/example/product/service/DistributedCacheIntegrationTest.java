package com.example.product.service;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class DistributedCacheIntegrationTest {

    @Autowired
    private ProductPriceService productPriceService;

    @MockitoBean
    private ProductRepository productRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        Product product = Product.builder()
                .id("P001")
                .name("Flash Sale Laptop")
                .price(15000000)
                .build();

        when(productRepository.findById("P001")).thenReturn(Optional.of(product));
    }

    @Test
    @DisplayName("Verify Spring Boot Context Loads with ProductPriceService & Cache Manager")
    void testContextLoads() {
        assertNotNull(productPriceService);
    }

    @Test
    @DisplayName("Verify getProductPrice fetches from repository")
    void testGetProductPrice() {
        Integer price = productPriceService.getProductPrice("P001");
        assertEquals(15000000, price);
        verify(productRepository, times(1)).findById("P001");
    }
}
