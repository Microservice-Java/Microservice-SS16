package com.example.product.controller;

import com.example.product.entity.Product;
import com.example.product.service.ProductPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductPriceController {

    private final ProductPriceService productPriceService;

    @GetMapping("/{productId}/price")
    public ResponseEntity<Map<String, Object>> getProductPrice(@PathVariable String productId) {
        Integer price = productPriceService.getProductPrice(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "price", price
        ));
    }

    @PutMapping("/{productId}/price")
    public ResponseEntity<Map<String, Object>> updateProductPrice(
            @PathVariable String productId,
            @RequestBody Map<String, Integer> request) {
        Integer newPrice = request.get("newPrice");
        productPriceService.updateProductPrice(productId, newPrice);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "newPrice", newPrice,
                "status", "UPDATED"
        ));
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productPriceService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }
}
