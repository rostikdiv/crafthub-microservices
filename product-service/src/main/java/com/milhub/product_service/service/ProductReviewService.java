package com.milhub.product_service.service;

import com.milhub.product_service.client.OrderServiceClient;
import com.milhub.product_service.client.UserServiceClient;
import com.milhub.product_service.dto.review.ProductReviewRequestDTO;
import com.milhub.product_service.dto.review.ProductReviewResponseDTO;
import com.milhub.product_service.dto.review.UserReviewHistoryDTO;
import com.milhub.product_service.entity.Product;
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

import java.util.*;
import java.util.function.Function;
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
     * Retrieves root reviews for a product with child replies fetched in a single batch query.
     */
    @Transactional(readOnly = true)
    public Page<ProductReviewResponseDTO> getReviewsByProduct(UUID productId, Pageable pageable) {
        Page<ProductReview> rootReviewsPage = reviewRepository.findAllRootReviewsByProductId(productId, pageable);
        if (rootReviewsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> rootIds = rootReviewsPage.getContent().stream()
                .map(ProductReview::getId)
                .toList();

        // Batch fetch all replies for the root reviews of the current page in a single query
        List<ProductReview> allReplies = reviewRepository.findAllByParentIdInOrderByCreatedAtAsc(rootIds);

        // Group replies by parent ID for fast O(1) in-memory tree assembly
        Map<UUID, List<ProductReview>> repliesByParentId = allReplies.stream()
                .filter(r -> r.getParent() != null)
                .collect(Collectors.groupingBy(r -> r.getParent().getId()));

        return rootReviewsPage.map(rootReview -> mapToDTOWithReplies(rootReview, repliesByParentId));
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
     * Retrieves review history for the current user using batch product lookups.
     */
    @Transactional(readOnly = true)
    public Page<UserReviewHistoryDTO> getUserReviewHistory(Pageable pageable) {
        UUID userId = userContext.getUserId();

        Page<ProductReview> reviewsPage = reviewRepository.findAllByUserId(userId, pageable);
        if (reviewsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Batch fetch all products referenced on this page in a single query
        Set<UUID> productIds = reviewsPage.getContent().stream()
                .map(ProductReview::getProductId)
                .collect(Collectors.toSet());

        Map<UUID, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return reviewsPage.map(review -> mapToHistoryDTO(review, productMap.get(review.getProductId())));
    }

    private UserReviewHistoryDTO mapToHistoryDTO(ProductReview review, Product product) {
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
            replyToText = (parentText != null && parentText.length() > 50)
                    ? parentText.substring(0, 50) + "..."
                    : parentText;
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
     * Hierarchical mapper for reviews with pre-fetched replies map.
     */
    private ProductReviewResponseDTO mapToDTOWithReplies(ProductReview review, Map<UUID, List<ProductReview>> repliesByParentId) {
        List<ProductReview> childReplies = repliesByParentId.getOrDefault(review.getId(), Collections.emptyList());
        List<ProductReviewResponseDTO> repliesDto = childReplies.stream()
                .map(child -> mapToDTOWithReplies(child, repliesByParentId))
                .collect(Collectors.toList());

        return new ProductReviewResponseDTO(
                review.getId(),
                review.getUserId(),
                review.getUserName(),
                review.getRating(),
                review.getComment(),
                review.isVerifiedPurchase(),
                review.getCreatedAt(),
                review.getParent() != null ? review.getParent().getId() : null,
                repliesDto);
    }

    /**
     * Fallback mapper for single review operations (e.g., creation).
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
                review.getReplies() != null
                        ? review.getReplies().stream().map(this::mapToDTO).collect(Collectors.toList())
                        : Collections.emptyList());
    }
}