package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.product.ProductRequestDTO;
import com.crafthub.product_service.dto.product.ProductResponseDTO;
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

/**
 * Controller for managing products, including CRUD operations, stock
 * management,
 * and discount applications.
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves a paginated list of products based on various filters.
     *
     * @param search      search term for product name or description
     * @param categoryId  filter by category identifier
     * @param minPrice    minimum price filter
     * @param maxPrice    maximum price filter
     * @param isAvailable filter items currently in stock
     * @param minRating   filter products by minimum average rating
     * @param sellerId    filter products belonging to a specific seller
     * @param pageable    pagination and sorting parameters
     * @return page of matching products
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) String accessLevel,

            // Sorting defaults to: in-stock first (quantity DESC), then newest first (createdAt DESC)
            @PageableDefault(sort = {"quantity", "createdAt"}, direction = Sort.Direction.DESC, size = 10) Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(
                search, categoryId, minPrice, maxPrice, isAvailable, minRating, sellerId, accessLevel, pageable));
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
            @RequestBody ProductRequestDTO request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PostMapping("/{id}/discount")
    @PreAuthorize("hasAuthority('product:update')") // Or check if the user is the product owner
    public ResponseEntity<ProductResponseDTO> applyDiscount(
            @PathVariable UUID id,
            @RequestParam BigDecimal newPrice) {
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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}