package com.example.product.service;

import com.example.product.entity.Product;
import com.example.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductPriceService {

    public static final String CACHE_NAME = "product_prices";

    private final ProductRepository productRepository;

    /**
     * Retrieves the price of a product.
     * Uses Redis distributed cache with key 'product_prices::<productId>'.
     * Condition ensures null or blank product IDs bypass cache and fail fast.
     */
    @Cacheable(value = CACHE_NAME, key = "#productId", condition = "#productId != null && !#productId.trim().isEmpty()", unless = "#result == null")
    public Integer getProductPrice(String productId) {
        validateProductId(productId);
        log.info("[DB Query] Fetching price from database for productId: {}", productId);

        return productRepository.findById(productId)
                .map(Product::getPrice)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));
    }

    /**
     * Updates the price of a product in the database and evicts the corresponding entry in Redis distributed cache.
     */
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#productId", condition = "#productId != null && !#productId.trim().isEmpty()")
    public void updateProductPrice(String productId, Integer newPrice) {
        validateProductId(productId);
        if (newPrice == null || newPrice < 0) {
            throw new IllegalArgumentException("New price must be non-negative");
        }

        log.info("[DB Update & Cache Evict] Updating price in database to {} for productId: {}", newPrice, productId);

        Product product = productRepository.findById(productId)
                .orElseGet(() -> Product.builder().id(productId).name("Product " + productId).build());

        product.setPrice(newPrice);
        productRepository.save(product);
    }

    /**
     * Helper method to save/initialize a product.
     */
    @Transactional
    public Product saveProduct(Product product) {
        if (product == null || product.getId() == null || product.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product and Product ID must not be null or blank");
        }
        return productRepository.save(product);
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID must not be null or blank");
        }
    }
}
