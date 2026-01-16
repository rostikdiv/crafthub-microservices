package com.crafthub.product_service.service;

import com.crafthub.product_service.client.UserServiceClient;
import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.entity.enums.AccessLevel;
import com.crafthub.product_service.entity.Category;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.repository.CategoryRepository;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.security.JwtParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final JwtParserService jwtParserService;
    private final UserServiceClient userServiceClient;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        String token = getTokenFromRequest();

        // 1. Дістаємо ID та Роль з токена
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
        if ("BUYER".equalsIgnoreCase(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Buyers cannot create products");
        }

        // 3. ✅ Отримуємо дані про продавця з User Service (Денормалізація)
        String sellerName = "Unknown Seller";
        String sellerLogo = null;

        try {
            // Робимо синхронний запит через Feign Client
            var sellerInfo = userServiceClient.getSellerInfo(userId);
            if (sellerInfo != null) {
                sellerName = sellerInfo.companyName();
                sellerLogo = sellerInfo.logoUrl();
            }
        } catch (Exception e) {
            // Якщо User Service недоступний або сталася помилка - логуємо, але не блокуємо створення товару
            log.warn("Could not fetch seller info for user {}: {}", userId, e.getMessage());
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        AccessLevel accessLevel = AccessLevel.valueOf(request.accessLevel().toUpperCase());

        // 4. Створення товару з новими полями
        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .category(category)
                .accessLevel(accessLevel)
                .sellerId(userId)
                .sellerName(sellerName)        // ✅ Зберігаємо ім'я
                .sellerLogoUrl(sellerLogo)     // ✅ Зберігаємо лого
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

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToProductResponse).toList();
    }

    public ProductResponseDTO getProductById(UUID id) {
        return productRepository.findById(id)
                .map(this::mapToProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public List<ProductResponseDTO> getProductsByIds(List<UUID> ids) {
        return productRepository.findAllById(ids).stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Transactional
    public void reduceStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        if (product.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.getName());
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
        log.info("📉 Stock reduced for product {}: -{} (New balance: {})", product.getName(), quantity, product.getQuantity());
    }

    @Transactional
    public void restoreStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);
        log.info("📈 Stock restored for product {}: +{} (New balance: {})", product.getName(), quantity, product.getQuantity());
    }

    // ✅ Оновлений маппер
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
                product.getSellerName(),    // ✅ Передаємо в DTO
                product.getSellerLogoUrl(), // ✅ Передаємо в DTO

                product.getWeight(),
                product.getLength(),
                product.getWidth(),
                product.getHeight(),
                product.getPreviewImageUrl(),
                product.getImageUrls()
        );
    }

    private String getTokenFromRequest() {
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (requestAttributes == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No request context");
        }

        HttpServletRequest request = requestAttributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        return authHeader;
    }
}