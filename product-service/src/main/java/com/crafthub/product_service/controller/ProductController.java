package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO; // ❗️ Імпорт DTO
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // ❗️ Імпорт PathVariable

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        // (Цей ендпоінт ми потім теж переведемо на DTO)
        return ResponseEntity.ok(productRepository.findAll());
    }

    // --- ❗️ НОВИЙ МЕТОД ДЛЯ FEIGN-КЛІЄНТА ---
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable String id) {
        return productRepository.findById(Long.parseLong(id)) // ❗️ TODO: Додати обробку NumberFormatException
                .map(product -> new ProductResponseDTO( // ❗️ Конвертація в DTO
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getStockQuantity()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // ❗️ Обробка 404
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO
    ) {
        Product createdProduct = productService.createProduct(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }
}