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

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    // 1. Додати відгук або відповідь
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()") // Будь-який залогінений юзер може писати (логіка verified всередині)
    public ResponseEntity<ProductReviewResponseDTO> addReview(
            @RequestBody @Valid ProductReviewRequestDTO request
    ) {
        return ResponseEntity.ok(reviewService.addReview(request));
    }

    // 2. Отримати відгуки для товару (Деревоподібний вигляд)
    // Публічний доступ (не треба авторизації)
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ProductReviewResponseDTO>> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    // 3. Отримати історію МОЇХ відгуків (Плоский вигляд для кабінету)
    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<UserReviewHistoryDTO>> getMyReviewHistory(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC, size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getUserReviewHistory(pageable));
    }
}