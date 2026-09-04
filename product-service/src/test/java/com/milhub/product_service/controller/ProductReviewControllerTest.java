package com.milhub.product_service.controller;

import com.milhub.product_service.dto.review.ProductReviewRequestDTO;
import com.milhub.product_service.dto.review.ProductReviewResponseDTO;
import com.milhub.product_service.dto.review.UserReviewHistoryDTO;
import com.milhub.product_service.service.ProductReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewControllerTest {

    @Mock
    private ProductReviewService reviewService;

    @InjectMocks
    private ProductReviewController reviewController;

    private UUID productId;
    private ProductReviewResponseDTO reviewDTO;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        reviewDTO = new ProductReviewResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Soldier77", 5,
                "Excellent gear", true, LocalDateTime.now(), null, List.of()
        );
    }

    @Test
    @DisplayName("addReview: adds review and returns OK")
    void addReview_ShouldReturnOk() {
        ProductReviewRequestDTO request = new ProductReviewRequestDTO(productId, 5, "Excellent gear", null);
        when(reviewService.addReview(request)).thenReturn(reviewDTO);

        ResponseEntity<ProductReviewResponseDTO> response = reviewController.addReview(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().comment()).isEqualTo("Excellent gear");
        verify(reviewService).addReview(request);
    }

    @Test
    @DisplayName("getProductReviews: returns page of reviews for product")
    void getProductReviews_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewService.getReviewsByProduct(productId, pageable)).thenReturn(new PageImpl<>(List.of(reviewDTO)));

        ResponseEntity<Page<ProductReviewResponseDTO>> response = reviewController.getProductReviews(productId, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getMyReviewHistory: returns authenticated user's review history")
    void getMyReviewHistory_ShouldReturnHistoryPage() {
        Pageable pageable = PageRequest.of(0, 10);
        UserReviewHistoryDTO historyDTO = new UserReviewHistoryDTO(
                UUID.randomUUID(), "Review comment", 5, LocalDateTime.now(),
                productId, "Tactical Boots", "http://boots.png", false, null, null
        );
        when(reviewService.getUserReviewHistory(pageable)).thenReturn(new PageImpl<>(List.of(historyDTO)));

        ResponseEntity<Page<UserReviewHistoryDTO>> response = reviewController.getMyReviewHistory(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent().get(0).productName()).isEqualTo("Tactical Boots");
    }
}
