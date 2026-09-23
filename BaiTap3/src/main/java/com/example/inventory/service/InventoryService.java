package com.example.inventory.service;

import com.example.inventory.config.RedisConfig;
import com.example.inventory.dto.ProductInventoryDTO;
import com.example.inventory.entity.ProductInventory;
import com.example.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * Cache-Aside Read Operation:
     * 1. Checks Redis cache under 'inventory::<productId>'.
     * 2. If Cache Miss, queries Database and populates Redis cache.
     * 3. Returns ProductInventoryDTO.
     */
    @Cacheable(
        value = RedisConfig.INVENTORY_CACHE,
        key = "#productId",
        condition = "#productId != null && !#productId.trim().isEmpty()",
        unless = "#result == null"
    )
    public ProductInventoryDTO getInventory(String productId) {
        validateProductId(productId);
        log.info("[Cache-Aside Read: DB Query] Fetching inventory from Database for productId: {}", productId);

        return inventoryRepository.findById(productId)
                .map(this::convertToDTO)
                .orElse(null);
    }

    /**
     * Cache-Aside Write Operation:
     * 1. Validates inputs (Fail-fast check for negative quantity or blank productId).
     * 2. Updates Database primary data source.
     * 3. Evicts stale entry from Redis cache (@CacheEvict).
     */
    @Transactional
    @CacheEvict(
        value = RedisConfig.INVENTORY_CACHE,
        key = "#productId",
        condition = "#productId != null && !#productId.trim().isEmpty()"
    )
    public ProductInventoryDTO updateInventory(String productId, Integer newQuantity) {
        validateProductId(productId);
        if (newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("Inventory quantity cannot be negative");
        }

        log.info("[Cache-Aside Write: DB Update & Evict] Updating DB quantity to {} for productId: {}", newQuantity, productId);

        ProductInventory inventory = inventoryRepository.findById(productId)
                .orElseGet(() -> ProductInventory.builder()
                        .productId(productId)
                        .productName("Product " + productId)
                        .build());

        inventory.setQuantity(newQuantity);
        ProductInventory saved = inventoryRepository.save(inventory);

        return convertToDTO(saved);
    }

    @Transactional
    public ProductInventoryDTO saveInventory(ProductInventory inventory) {
        if (inventory == null || inventory.getProductId() == null || inventory.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product Inventory and Product ID must not be null or blank");
        }
        if (inventory.getQuantity() != null && inventory.getQuantity() < 0) {
            throw new IllegalArgumentException("Inventory quantity cannot be negative");
        }
        ProductInventory saved = inventoryRepository.save(inventory);
        return convertToDTO(saved);
    }

    private ProductInventoryDTO convertToDTO(ProductInventory inventory) {
        return ProductInventoryDTO.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .quantity(inventory.getQuantity())
                .build();
    }

    private void validateProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID must not be null or blank");
        }
    }
}
