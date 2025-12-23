package com.crafthub.product_service.service;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.entity.Category; // 🆕
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.entity.enums.AccessLevel;
import com.crafthub.product_service.repository.CategoryRepository; // 🆕
import com.crafthub.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository; // 🆕 Інжектимо репозиторій

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        // 1. Спочатку шукаємо категорію
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + request.categoryId()));

        // 2. Створюємо товар
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .category(category) // 🔄 Сетимо об'єкт, а не рядок
                .imageUrl(request.imageUrl())
                .accessLevel(request.accessLevel() != null ? request.accessLevel() : AccessLevel.PUBLIC)
                .sellerId(request.sellerId())
                .build();

        productRepository.save(product);
        log.info("Product created: {}", product.getId());
        return mapToResponse(product);
    }

    public ProductResponseDTO getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToResponse(product);
    }

    private ProductResponseDTO mapToResponse(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .categoryName(product.getCategory().getName()) // 🔄 Беремо ім'я з об'єкта
                .categoryId(product.getCategory().getId())     // 🔄 Беремо ID з об'єкта
                .imageUrl(product.getImageUrl())
                .accessLevel(product.getAccessLevel())
                .sellerId(product.getSellerId())
                .build();
    }
}