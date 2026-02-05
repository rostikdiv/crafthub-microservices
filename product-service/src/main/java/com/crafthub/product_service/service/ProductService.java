package com.crafthub.product_service.service;

import com.crafthub.product_service.client.UserServiceClient;
import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.dto.SellerInfoDTO; // Переконайтесь, що цей імпорт є
import com.crafthub.product_service.entity.Category;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.entity.enums.AccessLevel;
import com.crafthub.product_service.exception.BusinessException;
import com.crafthub.product_service.exception.ResourceNotFoundException;
import com.crafthub.product_service.repository.CategoryRepository;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.repository.specification.ProductSpecification; // Ваш новий імпорт
import com.crafthub.product_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserContextService userContext;
    private final UserServiceClient userServiceClient;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        UUID userId = userContext.getUserId();

        SellerInfoDTO sellerInfo;
        try {
            // 🛑 Прямий виклик. Якщо User Service лежить - ми хочемо впасти, а не зберегти "Unknown"
            sellerInfo = userServiceClient.getSellerInfo(userId);
        } catch (Exception e) {
            log.error("Failed to fetch seller info during product creation for user {}: {}", userId, e.getMessage());
            // Повертаємо зрозумілу помилку користувачу
            throw new BusinessException("Неможливо створити товар: не вдалося отримати профіль продавця. Спробуйте пізніше.");
        }

        // Перевірка (опціонально): чи дозволено цьому продавцю створювати товари
        // if (!sellerInfo.isVerified()) { ... }

        return saveProductInternal(request, userId, sellerInfo);
    }

    // ✅ НОВИЙ МЕТОД: Масове створення
    @Transactional
    public List<ProductResponseDTO> createProducts(List<ProductRequestDTO> requests) {
        UUID userId = userContext.getUserId();

        SellerInfoDTO sellerInfo;
        try {
            // Те саме для масового створення
            sellerInfo = userServiceClient.getSellerInfo(userId);
        } catch (Exception e) {
            log.error("Batch creation failed. User Service unavailable for user {}", userId);
            throw new BusinessException("Неможливо створити товари: сервіс користувачів недоступний.");
        }

        log.info("Batch creating {} products for User ID: {}", requests.size(), userId);

        // sellerInfo тут вже гарантовано не null
        final SellerInfoDTO finalSellerInfo = sellerInfo;

        return requests.stream()
                .map(req -> saveProductInternal(req, userId, finalSellerInfo))
                .collect(Collectors.toList());
    }

    private ProductResponseDTO saveProductInternal(ProductRequestDTO request, UUID userId, SellerInfoDTO sellerInfo) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        AccessLevel accessLevel = AccessLevel.PUBLIC;
        if (request.accessLevel() != null) {
            try {
                accessLevel = AccessLevel.valueOf(request.accessLevel().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid access level: {}. Defaulting to PUBLIC", request.accessLevel());
            }
        }

        String sellerName = (sellerInfo != null) ? sellerInfo.companyName() : "Unknown Seller";
        String sellerLogo = (sellerInfo != null) ? sellerInfo.logoUrl() : null;

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .category(category)
                .accessLevel(accessLevel)
                .sellerId(userId)
                .sellerName(sellerName)
                .sellerLogoUrl(sellerLogo)
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

    // ... (getAllProducts, reduceStock, mapToProductResponse та інші методи залишаються без змін) ...
    // Не забудьте додати оновлений getAllProducts з пагінацією, який ми обговорювали раніше.
    public Page<ProductResponseDTO> getAllProducts(
            String search,
            Long categoryId, // Зверніть увагу, у вашому коді було Long
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isAvailable,
            Double minRating,
            Pageable pageable
    ) {
        // Викликаємо оновлений метод специфікації
        Specification<Product> spec = ProductSpecification.filterProducts(
                categoryId,
                minPrice,
                maxPrice,
                search,
                isAvailable,
                minRating
        );

        return productRepository.findAll(spec, pageable)
                .map(this::mapToProductResponse);
    }

    // ... решта існуючих методів ...
    private ProductResponseDTO mapToProductResponse(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getOldPrice(),
                product.getQuantity(),
                product.getCategory() != null ? product.getCategory().getName() : "No Category",
                product.getAccessLevel().name(),
                product.getSellerId(),
                product.getSellerName(),
                product.getSellerLogoUrl(),
                product.getAverageRating() != null ? product.getAverageRating() : 0.0,
                product.getReviewCount() != null ? product.getReviewCount() : 0,
                product.getWeight(),
                product.getLength(),
                product.getWidth(),
                product.getHeight(),
                product.getPreviewImageUrl(),
                product.getImageUrls()
        );
    }

    public ProductResponseDTO getProductById(UUID id) {
        return productRepository.findById(id)
                .map(this::mapToProductResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public List<ProductResponseDTO> getProductsByIds(List<UUID> ids) {
        return productRepository.findAllById(ids).stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Transactional
    public ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 1. Оновлення базових полів (якщо вони передані)
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.quantity() != null) product.setQuantity(request.quantity());
        if (request.accessLevel() != null) {
            try {
                product.setAccessLevel(AccessLevel.valueOf(request.accessLevel()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid access level");
            }
        }

        // 2. Оновлення Габаритів
        if (request.weight() != null) product.setWeight(request.weight());
        if (request.length() != null) product.setLength(request.length());
        if (request.width() != null) product.setWidth(request.width());
        if (request.height() != null) product.setHeight(request.height());

        // 3. Логіка зміни ціни (зі скиданням знижки)
        if (request.price() != null && request.price().compareTo(product.getPrice()) != 0) {
            product.setPrice(request.price());
            product.setOldPrice(null); // Скидаємо стару ціну, бо базова змінилася
        }

        // 4. Оновлення Зображень (Просте присвоєння URL)
        if (request.previewImageUrl() != null && !request.previewImageUrl().isBlank()) {
            product.setPreviewImageUrl(request.previewImageUrl());
        }

        if (request.imageUrls() != null) {
            // Можна замінити список повністю
            product.setImageUrls(request.imageUrls());
        }

        // 5. Оновлення категорії
        if (request.categoryId() != null && !request.categoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        productRepository.save(product);
        return mapToProductResponse(product);
    }

    @Transactional
    public void reduceStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
    }

    @Transactional
    public void restoreStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);
    }

    // У метод оновлення товару (updateProduct) або окремий метод (applyDiscount)

    @Transactional
    public ProductResponseDTO applyDiscount(UUID productId, BigDecimal newDiscountPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Валідація: Нова ціна має бути меншою за поточну (або стару, якщо вона вже є)
        BigDecimal originalPrice = (product.getOldPrice() != null) ? product.getOldPrice() : product.getPrice();

        if (newDiscountPrice.compareTo(originalPrice) >= 0) {
            throw new BusinessException("Discount price must be lower than original price (" + originalPrice + ")");
        }

        if (newDiscountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Price must be greater than 0");
        }

        // Якщо це перша знижка -> зберігаємо поточну ціну як стару
        if (product.getOldPrice() == null) {
            product.setOldPrice(product.getPrice());
        }

        // Встановлюємо нову ціну продажу
        product.setPrice(newDiscountPrice);

        productRepository.save(product);
        return mapToProductResponse(product); // Не забудьте оновити маппер!
    }

    @Transactional
    public ProductResponseDTO removeDiscount(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Якщо знижка була -> повертаємо стару ціну
        if (product.getOldPrice() != null) {
            product.setPrice(product.getOldPrice()); // Повертаємо 1000
            product.setOldPrice(null);               // Очищаємо поле
            productRepository.save(product);
        }

        return mapToProductResponse(product);
    }
}