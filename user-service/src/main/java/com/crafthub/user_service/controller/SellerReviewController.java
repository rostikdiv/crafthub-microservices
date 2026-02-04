package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.review.SellerReviewRequestDTO;
import com.crafthub.user_service.dto.review.SellerReviewResponseDTO;
import com.crafthub.user_service.service.SellerReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seller-reviews")
@RequiredArgsConstructor
public class SellerReviewController {

    private final SellerReviewService reviewService;

    @PostMapping
    public ResponseEntity<SellerReviewResponseDTO> addReview(@RequestBody SellerReviewRequestDTO request) {
        return ResponseEntity.ok(reviewService.addReview(request));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<SellerReviewResponseDTO>> getReviews(
            @PathVariable UUID sellerId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsBySeller(sellerId, pageable));
    }
}