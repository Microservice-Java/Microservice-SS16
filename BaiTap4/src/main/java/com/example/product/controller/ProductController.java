package com.example.product.controller;

import com.example.product.dto.ProductDTO;
import com.example.product.dto.UpdateProductRequestDTO;
import com.example.product.entity.Product;
import com.example.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String productId) {
        ProductDTO dto = productService.getProductById(productId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable String productId,
            @RequestBody UpdateProductRequestDTO request) {
        ProductDTO updated = productService.updateProduct(productId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody Product product) {
        ProductDTO saved = productService.saveProduct(product);
        return ResponseEntity.ok(saved);
    }
}
