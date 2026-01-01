package com.crafthub.product_service.service;

import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.entity.enums.AccessLevel;
import com.crafthub.product_service.entity.Category;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.CategoryRepository;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.security.JwtParserService; // ✅ Наш новий сервіс
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest; // ✅
import org.springframework.web.context.request.RequestContextHolder; // ✅
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final JwtParserService jwtParserService; // Використовуємо JWT парсер

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        String token = getTokenFromRequest();

        // 1. Дістаємо ID та Роль прямо з токена (без запиту до User Service)
        UUID userId;
        String userRole;
        try {
            userId = jwtParserService.extractUserId(token);
            userRole = jwtParserService.extractUserRole(token);
        } catch (Exception e) {
            log.error("Invalid Token", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT Token");
        }

        log.info("Creating product by User ID: {}, Role: {}", userId, userRole);

        // 2. Валідація ролі
        // У User Service роль зберігається як "BUYER", "SELLER" тощо.
        // Перевір, чи це не "BUYER"
        if ("BUYER".equalsIgnoreCase(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyers cannot create products");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        AccessLevel accessLevel = AccessLevel.valueOf(request.accessLevel().toUpperCase());

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .category(category)
                .accessLevel(accessLevel)
                .sellerId(userId)
                .weight(request.weight())
                .length(request.length())
                .width(request.width())
                .height(request.height())
                .previewImageUrl(request.previewImageUrl())
                .imageUrls(request.imageUrls() != null ? request.imageUrls() : List.of())
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    // ... інші методи без змін
    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToProductResponse).toList();
    }

    public ProductResponseDTO getProductById(UUID id) {
        return productRepository.findById(id)
                .map(this::mapToProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private ProductResponseDTO mapToProductResponse(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory() != null ? product.getCategory().getName() : "No Category",
                product.getAccessLevel().name(),
                product.getSellerId(),
                product.getWeight(),
                product.getLength(),
                product.getWidth(),
                product.getHeight(),
                product.getPreviewImageUrl(),
                product.getImageUrls()
        );
    }

    private String getTokenFromRequest() {
        // RequestContextHolder зберігає дані поточного потоку (ThreadLocal)
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No request context");
        }

        HttpServletRequest request = requestAttributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        return authHeader; // Повертає рядок "Bearer eyJhbGci..."
    }
}