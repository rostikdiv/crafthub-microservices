package com.milhub.product_service.service;

import com.milhub.product_service.client.OrderServiceClient;
import com.milhub.product_service.client.UserServiceClient;
import com.milhub.product_service.dto.review.ProductReviewRequestDTO;
import com.milhub.product_service.dto.review.ProductReviewResponseDTO;
import com.milhub.product_service.dto.review.UserReviewHistoryDTO;
import com.milhub.product_service.entity.ProductReview;
import com.milhub.product_service.exception.ResourceNotFoundException;
import com.milhub.product_service.repository.ProductRepository;
import com.milhub.product_service.repository.ProductReviewRepository;
import com.milhub.product_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing product reviews, replies, and rating calculations.
 */
@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderServiceIntegration orderServiceIntegration;
    private final UserContextService userContext;
    private final UserServiceClient userServiceClient; // To retrieve user name if needed

    /**
     * Adds a new review or reply.
     * Extracts user ID from context and calculates whether it's a verified
     * purchase.
     *
     * @param request review details
     * @return created review response
     */
    @Transactional
    @CacheEvict(value = "products", key = "#request.productId()")
    public ProductReviewResponseDTO addReview(ProductReviewRequestDTO request) {
        UUID userId = userContext.getUserId();
        // User name can be fetched from token or UserServiceClient
        String userName = "User " + userId.toString().substring(0, 5);

        // 1. Check if this is a root review or a reply
        ProductReview parent = null;
        if (request.parentId() != null) {
            parent = reviewRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent review not found"));

            // Validation: cannot reply to a comment for a different product
            if (!parent.getProductId().equals(request.productId())) {
                throw new IllegalArgumentException("Product ID mismatch");
            }
        }

        // 2. "Verified Purchase" check (for the author, regardless of parent)
        // Check if this specific user has purchased the product
        Boolean isVerified = orderServiceIntegration.checkPurchase(request.productId());
        if (isVerified == null)
            isVerified = false;

        // 3. Creation
        ProductReview review = ProductReview.builder()
                .productId(request.productId())
                .userId(userId)
                .userName(userName)
                .rating(request.rating()) // For replies, rating might be null if not provided by front-end
                .comment(request.comment())
                .isVerifiedPurchase(isVerified)
                .parent(parent)
                .build();

        reviewRepository.save(review);

        // 4. Update product rating (ONLY if it's a root review with a rating)
        if (parent == null && request.rating() != null) {
            updateProductRating(request.productId());
        }

        return mapToDTO(review);
    }

    /**
     * Retrieves root reviews for a product. Children are fetched via recursion in
     * mapToDTO.
     */
    public Page<ProductReviewResponseDTO> getReviewsByProduct(UUID productId, Pageable pageable) {
        // Fetch only top-level reviews; recursion handles children
        return reviewRepository.findAllRootReviewsByProductId(productId, pageable)
                .map(this::mapToDTO);
    }

    /**
     * Updates the aggregate rating and review count for a product.
     */
    private void updateProductRating(UUID productId) {
        // 1. Fetch aggregated data from DB
        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.getReviewCountByProductId(productId);

        // 2. Handle nulls (e.g., all reviews deleted or none exist)
        double newRating = (averageRating != null) ? averageRating : 0.0;
        int newCount = (reviewCount != null) ? reviewCount.intValue() : 0;

        // 3. Round to 1 decimal place (e.g., 4.6666 -> 4.7)
        newRating = Math.round(newRating * 10.0) / 10.0;

        // 4. Update product
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setAverageRating(newRating);
        product.setReviewCount(newCount);

        productRepository.saveAndFlush(product);
    }

    /**
     * Retrieves review history for the current user.
     */
    @Transactional(readOnly = true)
    public Page<UserReviewHistoryDTO> getUserReviewHistory(Pageable pageable) {
        UUID userId = userContext.getUserId();

        return reviewRepository.findAllByUserId(userId, pageable)
                .map(this::mapToHistoryDTO);
    }

    private UserReviewHistoryDTO mapToHistoryDTO(ProductReview review) {
        // Fetch product info for name and image
        // Cache or batch-fetch recommended for high volume
        var product = productRepository.findById(review.getProductId()).orElse(null);
        String productName = (product != null) ? product.getName() : "Unknown Product";
        String productImg = (product != null) ? product.getPreviewImageUrl() : null;

        // Logic for determining reply context
        boolean isReply = review.getParent() != null;
        String replyToUser = null;
        String replyToText = null;

        if (isReply) {
            replyToUser = review.getParent().getUserName();
            // Truncate parent comment if it's too long
            String parentText = review.getParent().getComment();
            replyToText = parentText.length() > 50 ? parentText.substring(0, 50) + "..." : parentText;
        }

        return new UserReviewHistoryDTO(
                review.getId(),
                review.getComment(),
                review.getRating(),
                review.getCreatedAt(),
                review.getProductId(),
                productName,
                productImg,
                isReply,
                replyToUser,
                replyToText);
    }

    /**
     * Recursive mapper for reviews and their replies.
     */
    private ProductReviewResponseDTO mapToDTO(ProductReview review) {
        return new ProductReviewResponseDTO(
                review.getId(),
                review.getUserId(),
                review.getUserName(),
                review.getRating(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getCreatedAt(),
                review.getParent() != null ? review.getParent().getId() : null,
                review.getReplies().stream().map(this::mapToDTO).collect(Collectors.toList()));
    }
}