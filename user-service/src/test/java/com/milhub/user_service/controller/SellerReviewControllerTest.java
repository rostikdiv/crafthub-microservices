package com.milhub.user_service.controller;

import com.milhub.user_service.dto.review.SellerReviewRequestDTO;
import com.milhub.user_service.dto.review.SellerReviewResponseDTO;
import com.milhub.user_service.service.SellerReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerReviewControllerTest {

    @Mock
    private SellerReviewService reviewService;

    @InjectMocks
    private SellerReviewController controller;

    @Test
    void addReview_ShouldCallService() {
        UUID sellerId = UUID.randomUUID();
        SellerReviewRequestDTO request = new SellerReviewRequestDTO(sellerId, 5, "Great seller");
        SellerReviewResponseDTO responseDTO = new SellerReviewResponseDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Buyer", 5, "Great seller", LocalDateTime.now()
        );

        when(reviewService.addReview(request)).thenReturn(responseDTO);

        ResponseEntity<SellerReviewResponseDTO> response = controller.addReview(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(reviewService).addReview(request);
    }

    @Test
    void getReviews_ShouldCallService() {
        UUID sellerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<SellerReviewResponseDTO> page = new PageImpl<>(List.of());

        when(reviewService.getReviewsBySeller(sellerId, pageable)).thenReturn(page);

        ResponseEntity<Page<SellerReviewResponseDTO>> response = controller.getReviews(sellerId, pageable);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(reviewService).getReviewsBySeller(sellerId, pageable);
    }
}
