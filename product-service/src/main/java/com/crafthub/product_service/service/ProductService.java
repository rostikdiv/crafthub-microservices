package com.crafthub.product_service.service;

import com.crafthub.product_service.client.UserServiceClient;
import com.crafthub.product_service.dto.ProductRequestDTO;
import com.crafthub.product_service.dto.ProductResponseDTO;
import com.crafthub.product_service.entity.Category;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.entity.enums.AccessLevel;
import com.crafthub.product_service.exception.ResourceNotFoundException; // ✅ Використовуємо наш новий Exception
import com.crafthub.product_service.repository.CategoryRepository;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserContextService userContext; // ✅ Наше джерело даних про юзера
    private final UserServiceClient userServiceClient;

    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        // 1. Беремо ID з заголовків (через UserContextService)
        UUID userId = userContext.getUserId();
        String userRole = userContext.getUserRole();

        log.info("Creating product by User ID: {}, Role: {}", userId, userRole);

        // 2. Отримуємо дані про продавця
        String sellerName = "Unknown Seller";
        String sellerLogo = null;
        try {
            var sellerInfo = userServiceClient.getSellerInfo(userId);
            if (sellerInfo != null) {
                sellerName = sellerInfo.companyName();
                sellerLogo = sellerInfo.logoUrl();
            }
        } catch (Exception e) {
            log.warn("Could not fetch seller info for user {}: {}", userId, e.getMessage());
        }

        // 3. Пошук категорії (використовуємо наш новий Exception)
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        AccessLevel accessLevel = AccessLevel.valueOf(request.accessLevel().toUpperCase());

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

    public List<ProductResponseDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::mapToProductResponse).toList();
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
    public void reduceStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getQuantity() < quantity) {
            // Тут можна створити окремий InsufficientStockException
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
                product.getSellerName(),
                product.getSellerLogoUrl(),
                product.getWeight(),
                product.getLength(),
                product.getWidth(),
                product.getHeight(),
                product.getPreviewImageUrl(),
                product.getImageUrls()
        );
    }
}