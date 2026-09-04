package com.milhub.product_service.service;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderServiceIntegration orderServiceIntegration;

    @Mock
    private UserContextService userContext;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ProductReviewService productReviewService;

    private UUID userId;
    private UUID productId;
    private UUID reviewId;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        product = Product.builder()
                .id(productId)
                .name("Tactical Boots")
                .previewImageUrl("http://boots.png")
                .averageRating(0.0)
                .reviewCount(0)
                .build();
    }

    @Test
    @DisplayName("addReview: successfully adds root review with rating and updates product rating")
    void addReview_WhenRootReviewWithRating_ShouldSaveAndUpdateRating() {
        ProductReviewRequestDTO request = new ProductReviewRequestDTO(productId, 5, "Great boots!", null);

        when(userContext.getUserId()).thenReturn(userId);
        when(orderServiceIntegration.checkPurchase(productId)).thenReturn(true);
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> {
            ProductReview r = inv.getArgument(0);
            r.setId(reviewId);
            return r;
        });
        when(reviewRepository.getAverageRatingByProductId(productId)).thenReturn(4.8);
        when(reviewRepository.getReviewCountByProductId(productId)).thenReturn(1L);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductReviewResponseDTO response = productReviewService.addReview(request);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Great boots!");
        assertThat(response.isVerifiedPurchase()).isTrue();

        verify(productRepository).saveAndFlush(product);
        assertThat(product.getAverageRating()).isEqualTo(4.8);
        assertThat(product.getReviewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("addReview: successfully adds reply to existing review without updating rating")
    void addReview_WhenReply_ShouldSaveWithoutUpdatingProductRating() {
        UUID parentId = UUID.randomUUID();
        ProductReview parentReview = ProductReview.builder()
                .id(parentId)
                .productId(productId)
                .userId(UUID.randomUUID())
                .userName("Author")
                .comment("Original review")
                .build();

        ProductReviewRequestDTO replyRequest = new ProductReviewRequestDTO(productId, null, "Thanks for review!", parentId);

        when(userContext.getUserId()).thenReturn(userId);
        when(reviewRepository.findById(parentId)).thenReturn(Optional.of(parentReview));
        when(orderServiceIntegration.checkPurchase(productId)).thenReturn(false);
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> {
            ProductReview r = inv.getArgument(0);
            r.setId(reviewId);
            return r;
        });

        ProductReviewResponseDTO response = productReviewService.addReview(replyRequest);

        assertThat(response).isNotNull();
        assertThat(response.comment()).isEqualTo("Thanks for review!");
        assertThat(response.parentId()).isEqualTo(parentId);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("addReview: throws ResourceNotFoundException when parent review not found")
    void addReview_WhenParentNotFound_ShouldThrowResourceNotFoundException() {
        UUID parentId = UUID.randomUUID();
        ProductReviewRequestDTO replyRequest = new ProductReviewRequestDTO(productId, null, "Reply", parentId);

        when(userContext.getUserId()).thenReturn(userId);
        when(reviewRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productReviewService.addReview(replyRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parent review not found");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("addReview: throws IllegalArgumentException when parent review belongs to different product")
    void addReview_WhenParentProductMismatch_ShouldThrowIllegalArgumentException() {
        UUID parentId = UUID.randomUUID();
        ProductReview parentReview = ProductReview.builder()
                .id(parentId)
                .productId(UUID.randomUUID()) // different product
                .comment("Different product")
                .build();

        ProductReviewRequestDTO replyRequest = new ProductReviewRequestDTO(productId, null, "Reply", parentId);

        when(userContext.getUserId()).thenReturn(userId);
        when(reviewRepository.findById(parentId)).thenReturn(Optional.of(parentReview));

        assertThatThrownBy(() -> productReviewService.addReview(replyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product ID mismatch");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("getReviewsByProduct: returns empty page when no root reviews found")
    void getReviewsByProduct_WhenNoReviews_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reviewRepository.findAllRootReviewsByProductId(productId, pageable)).thenReturn(Page.empty());

        Page<ProductReviewResponseDTO> result = productReviewService.getReviewsByProduct(productId, pageable);

        assertThat(result).isEmpty();
        verify(reviewRepository, never()).findAllByParentIdInOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("getReviewsByProduct: returns hierarchical reviews with child replies")
    void getReviewsByProduct_WhenReviewsExist_ShouldReturnTreeStructure() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductReview rootReview = ProductReview.builder()
                .id(reviewId)
                .productId(productId)
                .userId(userId)
                .userName("User1")
                .rating(5)
                .comment("Super!")
                .isVerifiedPurchase(true)
                .createdAt(LocalDateTime.now())
                .build();

        UUID replyId = UUID.randomUUID();
        ProductReview replyReview = ProductReview.builder()
                .id(replyId)
                .productId(productId)
                .userId(UUID.randomUUID())
                .userName("Seller")
                .comment("Thank you!")
                .parent(rootReview)
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.findAllRootReviewsByProductId(productId, pageable))
                .thenReturn(new PageImpl<>(List.of(rootReview)));
        when(reviewRepository.findAllByParentIdInOrderByCreatedAtAsc(List.of(reviewId)))
                .thenReturn(List.of(replyReview));

        Page<ProductReviewResponseDTO> result = productReviewService.getReviewsByProduct(productId, pageable);

        assertThat(result).isNotEmpty();
        assertThat(result.getContent()).hasSize(1);
        ProductReviewResponseDTO rootDto = result.getContent().get(0);
        assertThat(rootDto.comment()).isEqualTo("Super!");
        assertThat(rootDto.replies()).hasSize(1);
        assertThat(rootDto.replies().get(0).comment()).isEqualTo("Thank you!");
    }

    @Test
    @DisplayName("getUserReviewHistory: returns empty page when no user reviews exist")
    void getUserReviewHistory_WhenEmpty_ShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userContext.getUserId()).thenReturn(userId);
        when(reviewRepository.findAllByUserId(userId, pageable)).thenReturn(Page.empty());

        Page<UserReviewHistoryDTO> result = productReviewService.getUserReviewHistory(pageable);

        assertThat(result).isEmpty();
        verify(productRepository, never()).findAllByIdIn(any());
    }

    @Test
    @DisplayName("getUserReviewHistory: maps user reviews and replies with truncated parent text")
    void getUserReviewHistory_WhenReviewsExist_ShouldMapToHistoryDTO() {
        Pageable pageable = PageRequest.of(0, 10);

        ProductReview parentReview = ProductReview.builder()
                .id(UUID.randomUUID())
                .userName("OriginalAuthor")
                .comment("This is a very long original review that definitely exceeds fifty characters for truncation test")
                .build();

        ProductReview userReply = ProductReview.builder()
                .id(reviewId)
                .productId(productId)
                .userId(userId)
                .comment("I agree!")
                .rating(null)
                .createdAt(LocalDateTime.now())
                .parent(parentReview)
                .build();

        when(userContext.getUserId()).thenReturn(userId);
        when(reviewRepository.findAllByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(userReply)));
        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(product));

        Page<UserReviewHistoryDTO> result = productReviewService.getUserReviewHistory(pageable);

        assertThat(result).isNotEmpty();
        UserReviewHistoryDTO historyDTO = result.getContent().get(0);
        assertThat(historyDTO.productName()).isEqualTo("Tactical Boots");
        assertThat(historyDTO.isReply()).isTrue();
        assertThat(historyDTO.replyToUserName()).isEqualTo("OriginalAuthor");
        assertThat(historyDTO.replyToText()).endsWith("...");
    }
}
