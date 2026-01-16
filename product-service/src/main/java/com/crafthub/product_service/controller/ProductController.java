package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/")
    public List<ProductResponseDTO> getAllProducts() {
        return productService.getAllProducts();
    }

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO createProduct(@RequestBody @Valid ProductRequestDTO productRequest) {
        return productService.createProduct(productRequest);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductResponseDTO>> getProductsBatch(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(@PathVariable UUID id) {
        return productService.getProductById(id);
    }


    @PostMapping("/{id}/reduce-stock")
    public ResponseEntity<Void> reduceStock(@PathVariable UUID id, @RequestParam Integer quantity) {
        productService.reduceStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore-stock")
    public ResponseEntity<Void> restoreStock(@PathVariable UUID id, @RequestParam Integer quantity) {
        productService.restoreStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("product service works!");
    }
}