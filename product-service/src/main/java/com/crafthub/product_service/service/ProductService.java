package com.crafthub.product_service.service;

import com.crafthub.product_service.client.UserServiceClient;
import com.crafthub.product_service.dto.product.ProductRequestDTO;
import com.crafthub.product_service.dto.product.ProductResponseDTO;
import com.crafthub.product_service.dto.SellerInfoDTO;
import com.crafthub.product_service.entity.Category;
import com.crafthub.product_service.entity.Product;
import com.crafthub.product_service.entity.enums.AccessLevel;
import com.crafthub.product_service.exception.BusinessException;
import com.crafthub.product_service.exception.ResourceNotFoundException;
import com.crafthub.product_service.repository.CategoryRepository;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.repository.specification.ProductSpecification;
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

/**
 * Core service for managing products, including CRUD operations, stock
 * management,
 * and filtering logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserContextService userContext;
    private final UserServiceClient userServiceClient;

    /**
     * Creates a new product for a seller.
     * Fetches seller information from the User Service to ensure profile existence.
     *
     * @param request product creation data
     * @return newly created product details
     */
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO request) {
        UUID userId = userContext.getUserId();

        SellerInfoDTO sellerInfo;
        try {
            // Direct call. If User Service is down, we want to fail rather than save
            // "Unknown"
            sellerInfo = userServiceClient.getSellerInfo(userId);
        } catch (Exception e) {
            log.error("Failed to fetch seller info during product creation for user {}: {}", userId, e.getMessage());
            // Return a user-friendly error
            throw new BusinessException(
                    "Unable to create product: failed to retrieve seller profile. Please try again later.");
        }

        // Check (optional): is the seller verified/allowed to post
        // if (!sellerInfo.isVerified()) { ... }

        return saveProductInternal(request, userId, sellerInfo);
    }

    /**
     * Batch creates multiple products for the current user.
     *
     * @param requests list of product creation requests
     * @return list of created product responses
     */
    @Transactional
    public List<ProductResponseDTO> createProducts(List<ProductRequestDTO> requests) {
        UUID userId = userContext.getUserId();

        SellerInfoDTO sellerInfo;
        try {
            // Same check for batch creation
            sellerInfo = userServiceClient.getSellerInfo(userId);
        } catch (Exception e) {
            log.error("Batch creation failed. User Service unavailable for user {}", userId);
            throw new BusinessException("Unable to create products: user service is unavailable.");
        }

        log.info("Batch creating {} products for User ID: {}", requests.size(), userId);

        // sellerInfo is guaranteed not null here
        final SellerInfoDTO finalSellerInfo = sellerInfo;

        return requests.stream()
                .map(req -> saveProductInternal(req, userId, finalSellerInfo))
                .collect(Collectors.toList());
    }

    /**
     * Internal helper to save product and map to response.
     */
    private ProductResponseDTO saveProductInternal(ProductRequestDTO request, UUID userId, SellerInfoDTO sellerInfo) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

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

    /**
     * Retrieves a paginated list of products matching the specified filters.
     */
    public Page<ProductResponseDTO> getAllProducts(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean isAvailable,
            Double minRating,
            UUID sellerId, // Add sellerId parameter
            Pageable pageable) {
        // Use the updated specification method
        Specification<Product> spec = ProductSpecification.filterProducts(
                categoryId,
                minPrice,
                maxPrice,
                search,
                isAvailable,
                minRating,
                sellerId); // Pass sellerId

        return productRepository.findAll(spec, pageable)
                .map(this::mapToProductResponse);
    }

    /**
     * Maps a Product entity to a ProductResponseDTO.
     */
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
                product.getImageUrls());
    }

    /**
     * Retrieves a specific product by its identifier.
     */
    public ProductResponseDTO getProductById(UUID id) {
        return productRepository.findById(id)
                .map(this::mapToProductResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /**
     * Retrieves multiple products by their identifiers.
     */
    public List<ProductResponseDTO> getProductsByIds(List<UUID> ids) {
        return productRepository.findAllById(ids).stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    /**
     * Updates an existing product's details.
     */
    @Transactional
    public ProductResponseDTO updateProduct(UUID productId, ProductRequestDTO request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 1. Update basic fields (if provided)
        if (request.name() != null)
            product.setName(request.name());
        if (request.description() != null)
            product.setDescription(request.description());
        if (request.quantity() != null)
            product.setQuantity(request.quantity());
        if (request.accessLevel() != null) {
            try {
                product.setAccessLevel(AccessLevel.valueOf(request.accessLevel()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid access level");
            }
        }

        // 2. Update Dimensions
        if (request.weight() != null)
            product.setWeight(request.weight());
        if (request.length() != null)
            product.setLength(request.length());
        if (request.width() != null)
            product.setWidth(request.width());
        if (request.height() != null)
            product.setHeight(request.height());

        // 3. Price change logic (resets old price if base price changes)
        if (request.price() != null && request.price().compareTo(product.getPrice()) != 0) {
            product.setPrice(request.price());
            product.setOldPrice(null); // Reset old price because base price changed
        }

        // 4. Image Update (Simple assignment)
        if (request.previewImageUrl() != null && !request.previewImageUrl().isBlank()) {
            product.setPreviewImageUrl(request.previewImageUrl());
        }

        if (request.imageUrls() != null) {
            // Replace list completely
            product.setImageUrls(request.imageUrls());
        }

        // 5. Category Update
        if (request.categoryId() != null && !request.categoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        productRepository.save(product);
        return mapToProductResponse(product);
    }

    /**
     * Reduces the stock quantity for a product.
     */
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

    /**
     * Restores the stock quantity for a product.
     */
    @Transactional
    public void restoreStock(UUID productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);
    }

    /**
     * Applies a discount price to a product.
     * Original price is saved in the oldPrice field if it's the first discount.
     */
    @Transactional
    public ProductResponseDTO applyDiscount(UUID productId, BigDecimal newDiscountPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Validation: New price must be lower than the original price
        BigDecimal originalPrice = (product.getOldPrice() != null) ? product.getOldPrice() : product.getPrice();

        if (newDiscountPrice.compareTo(originalPrice) >= 0) {
            throw new BusinessException("Discount price must be lower than original price (" + originalPrice + ")");
        }

        if (newDiscountPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Price must be greater than 0");
        }

        // If this is the first discount -> save current price as old price
        if (product.getOldPrice() == null) {
            product.setOldPrice(product.getPrice());
        }

        // Set the new sale price
        product.setPrice(newDiscountPrice);

        productRepository.save(product);
        return mapToProductResponse(product);
    }

    /**
     * Removes the discount and restores the original price from oldPrice.
     */
    @Transactional
    public ProductResponseDTO removeDiscount(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // If a discount was present -> restore old price
        if (product.getOldPrice() != null) {
            product.setPrice(product.getOldPrice()); // Restore price
            product.setOldPrice(null); // Clear the field
            productRepository.save(product);
        }

        return mapToProductResponse(product);
    }

    /**
     * Deletes a product by its identifier.
     */
    @Transactional
    public void deleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        productRepository.deleteById(productId);
    }
}