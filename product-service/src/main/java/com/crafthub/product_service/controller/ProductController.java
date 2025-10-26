package com.crafthub.product_service.controller;

import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
// ❗️ Шлях, який ми вже налаштували в Gateway
@RequestMapping("/api/v1/products/")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    // Тимчасовий "лакмусовий" ендпоінт для перевірки
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }
}