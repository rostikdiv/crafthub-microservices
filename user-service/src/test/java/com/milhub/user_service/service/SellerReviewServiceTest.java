package com.milhub.user_service.service;

import com.milhub.user_service.dto.review.SellerReviewRequestDTO;
import com.milhub.user_service.dto.review.SellerReviewResponseDTO;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.SellerReview;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.repository.SellerProfileRepository;
import com.milhub.user_service.repository.SellerReviewRepository;
import com.milhub.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerReviewServiceTest {

    @Mock
    private SellerReviewRepository reviewRepository;

    @Mock
    private SellerProfileRepository profileRepository;

    @Mock
    private OrderServiceIntegration orderServiceIntegration;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SellerReviewService sellerReviewService;

    private UUID buyerId;
    private UUID sellerId;
    private User buyerUser;
    private SellerProfile sellerProfile;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();

        buyerUser = User.builder()
                .id(buyerId)
                .firstName("Petro")
                .lastName("Poroshenko")
                .build();

        sellerProfile = SellerProfile.builder()
                .id(UUID.randomUUID())
                .rating(0.0f)
                .reviewCount(0)
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(buyerId.toString());
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should throw BusinessException when seller attempts to review themselves")
    void addReview_WhenReviewingSelf_ShouldThrowException() {
        SellerReviewRequestDTO request = new SellerReviewRequestDTO(buyerId, 5, "Great seller!");

        assertThatThrownBy(() -> sellerReviewService.addReview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("You cannot review yourself");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when buyer has not completed a purchase from seller")
    void addReview_WhenNotPurchased_ShouldThrowException() {
        SellerReviewRequestDTO request = new SellerReviewRequestDTO(sellerId, 5, "Great seller!");

        when(orderServiceIntegration.checkSellerPurchase(buyerId, sellerId)).thenReturn(false);

        assertThatThrownBy(() -> sellerReviewService.addReview(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("You can only review sellers you have purchased from");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add review, update average rating and total review count on seller profile")
    void addReview_Success_ShouldSaveReviewAndUpdateSellerStats() {
        SellerReviewRequestDTO request = new SellerReviewRequestDTO(sellerId, 5, "Fast delivery, excellent gear!");

        when(orderServiceIntegration.checkSellerPurchase(buyerId, sellerId)).thenReturn(true);
        when(reviewRepository.getAverageRating(sellerId)).thenReturn(4.8);
        when(reviewRepository.countBySellerId(sellerId)).thenReturn(10);
        when(profileRepository.findByUserId(sellerId)).thenReturn(Optional.of(sellerProfile));

        SellerReviewResponseDTO response = sellerReviewService.addReview(request);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Fast delivery, excellent gear!");

        verify(reviewRepository).save(any(SellerReview.class));
        assertThat(sellerProfile.getRating()).isEqualTo(4.8f);
        assertThat(sellerProfile.getReviewCount()).isEqualTo(10);
        verify(profileRepository).save(sellerProfile);
    }

    @Test
    @DisplayName("Should return paginated reviews for seller")
    void getReviewsBySeller_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        SellerReview r = SellerReview.builder()
                .id(UUID.randomUUID())
                .sellerId(sellerId)
                .userId(buyerId)
                .userName("Petro")
                .rating(5)
                .comment("Top quality")
                .build();

        Page<SellerReview> page = new PageImpl<>(List.of(r));
        when(reviewRepository.findAllBySellerId(sellerId, pageable)).thenReturn(page);

        Page<SellerReviewResponseDTO> result = sellerReviewService.getReviewsBySeller(sellerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reviewerName()).isEqualTo("Petro");
    }
}
