package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Оновлений GET з пагінацією
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Double minRating, // 👈 Фільтр рейтингу

            // Сортування працює саме: ?sort=averageRating,desc
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getAllProducts(
                search, categoryId, minPrice, maxPrice, isAvailable, minRating, pageable
        ));
    }

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product:create')")
    public ProductResponseDTO createProduct(@RequestBody @Valid ProductRequestDTO productRequest) {
        return productService.createProduct(productRequest);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product:create')")
    public List<ProductResponseDTO> createProductsBatch(@RequestBody @Valid List<ProductRequestDTO> productRequests) {
        return productService.createProducts(productRequests);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductResponseDTO>> getProductsBatch(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(productService.getProductsByIds(ids));
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(@PathVariable UUID id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:update')")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable UUID id,
            @RequestBody ProductRequestDTO request
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize("hasAuthority('product:update')") // Або перевірка власника
    public ResponseEntity<ProductResponseDTO> applyDiscount(
            @PathVariable UUID id,
            @RequestParam BigDecimal newPrice
    ) {
        return ResponseEntity.ok(productService.applyDiscount(id, newPrice));
    }

    @DeleteMapping("/{id}/discount")
    @PreAuthorize("hasAuthority('product:update')")
    public ResponseEntity<ProductResponseDTO> removeDiscount(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.removeDiscount(id));
    }

    @PostMapping("/{id}/reduce-stock")
    @PreAuthorize("hasAuthority('product:update') or hasAuthority('order:create')")
    public ResponseEntity<Void> reduceStock(@PathVariable UUID id, @RequestParam Integer quantity) {
        productService.reduceStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore-stock")
    @PreAuthorize("hasAuthority('product:update') or hasAuthority('order:create')")
    public ResponseEntity<Void> restoreStock(@PathVariable UUID id, @RequestParam Integer quantity) {
        productService.restoreStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("product service works!");
    }
}