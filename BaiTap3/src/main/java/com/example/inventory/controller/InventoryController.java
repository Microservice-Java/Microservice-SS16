package com.example.inventory.controller;

import com.example.inventory.dto.ProductInventoryDTO;
import com.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductInventoryDTO> getInventory(@PathVariable String productId) {
        ProductInventoryDTO dto = inventoryService.getInventory(productId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductInventoryDTO> updateInventory(
            @PathVariable String productId,
            @RequestBody Map<String, Integer> request) {
        Integer newQuantity = request.get("quantity");
        ProductInventoryDTO updated = inventoryService.updateInventory(productId, newQuantity);
        return ResponseEntity.ok(updated);
    }
}
