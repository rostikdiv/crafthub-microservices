package com.crafthub.product_service.controller;

import com.crafthub.product_service.dto.review.ProductReviewRequestDTO;
import com.crafthub.product_service.dto.review.ProductReviewResponseDTO;
import com.crafthub.product_service.dto.review.UserReviewHistoryDTO;
import com.crafthub.product_service.service.ProductReviewService;
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

import java.util.UUID;

/**
 * Controller for managing product reviews and replies.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    /**
     * Adds a new product review or a reply to an existing one.
     * Accessible to authenticated users.
     *
     * @param request the review request data
     * @return the newly created review details
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()") // Logic for verified purchase is handled inside the service
    public ResponseEntity<ProductReviewResponseDTO> addReview(
            @RequestBody @Valid ProductReviewRequestDTO request) {
        return ResponseEntity.ok(reviewService.addReview(request));
    }

    /**
     * Retrieves a paginated list of reviews for a specific product.
     * Publicly accessible, returns a tree-like structure.
     *
     * @param productId product identifier
     * @param pageable  pagination parameters
     * @return page of reviews
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ProductReviewResponseDTO>> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    /**
     * Retrieves the authenticated user's review history.
     * Returns a flat list optimized for the user profile view.
     *
     * @param pageable pagination parameters
     * @return page of user review history
     */
    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserReviewHistoryDTO>> getMyReviewHistory(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getUserReviewHistory(pageable));
    }
}