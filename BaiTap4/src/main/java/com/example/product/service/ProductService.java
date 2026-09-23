package com.example.product.service;

import com.example.product.config.RedisConfig;
import com.example.product.dto.ProductDTO;
import com.example.product.dto.UpdateProductRequestDTO;
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
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Read product by ID.
     * Uses @Cacheable for lazy loading cache under 'products::<productId>'.
     */
    @Cacheable(
        value = RedisConfig.PRODUCTS_CACHE,
        key = "#productId",
        condition = "#productId != null && !#productId.trim().isEmpty()",
        unless = "#result == null"
    )
    public ProductDTO getProductById(String productId) {
        validateProductId(productId);
        log.info("[DB Query] Fetching Product from Database for productId: {}", productId);

        return productRepository.findById(productId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Update product details using the optimal @CacheEvict strategy for 100:1 read/write ratio.
     * Updates Database first, then evicts (invalidates) stale Redis cache entry.
     */
    @Transactional
    @CacheEvict(
        value = RedisConfig.PRODUCTS_CACHE,
        key = "#productId",
        condition = "#productId != null && !#productId.trim().isEmpty()"
    )
    public ProductDTO updateProduct(String productId, UpdateProductRequestDTO request) {
        validateProductId(productId);
        if (request == null) {
            throw new IllegalArgumentException("Update request body must not be null");
        }
        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new IllegalArgumentException("Product price must be non-negative");
        }
        if (request.getStockQuantity() != null && request.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Product stock quantity must be non-negative");
        }

        log.info("[DB Update & Cache Evict] Updating DB product details for productId: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            product.setName(request.getName());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }

        Product saved = productRepository.save(product);
        return convertToDTO(saved);
    }

    @Transactional
    public ProductDTO saveProduct(Product product) {
        if (product == null || product.getId() == null || product.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product and Product ID must not be null or blank");
        }
        Product saved = productRepository.save(product);
        return convertToDTO(saved);
    }

    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .description(product.getDescription())
                .build();
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID must not be null or blank");
        }
    }
}
