package com.milhub.user_service.service;

import com.milhub.user_service.dto.review.SellerReviewRequestDTO;
import com.milhub.user_service.dto.review.SellerReviewResponseDTO;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.SellerReview;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.SellerProfileRepository;
import com.milhub.user_service.repository.SellerReviewRepository;
import com.milhub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for handling seller reviews, including authorization and rating
 * aggregation.
 */
@Service
@RequiredArgsConstructor
public class SellerReviewService {

    private final SellerReviewRepository reviewRepository;
    private final SellerProfileRepository profileRepository;
    private final OrderServiceIntegration orderServiceIntegration;
    private final UserRepository userRepository;

    /**
     * Adds a new review for a seller. Validates that the user is not reviewing
     * themselves
     * and has a verified purchase from the seller.
     *
     * @param request The review details.
     * @return The saved review details.
     */
    @Transactional
    public SellerReviewResponseDTO addReview(SellerReviewRequestDTO request) {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userId.equals(request.sellerId())) {
            System.err.println(
                    "Review Error: User " + userId + " tried to review self (Seller " + request.sellerId() + ")");
            throw new BusinessException("You cannot review yourself");
        }

        // Verification of purchase via communication with Order Service
        Boolean hasBought = orderServiceIntegration.checkSellerPurchase(userId, request.sellerId());
        if (!Boolean.TRUE.equals(hasBought)) {
            System.err.println("Review Error: Purchase verification failed for User " + userId + " and Seller "
                    + request.sellerId());
            throw new BusinessException("You can only review sellers you have purchased from (delivered orders).");
        }

        SellerReview review = SellerReview.builder()
                .sellerId(request.sellerId())
                .userId(userId)
                .userName(user.getFirstName() + " " + user.getLastName())
                .rating(request.rating())
                .comment(request.comment())
                .build();

        reviewRepository.save(review);

        // Immediate update of seller rating and review count
        updateSellerStats(request.sellerId());

        return mapToDTO(review);
    }

    /**
     * Recalculates and updates the average rating and review count for a specified
     * seller.
     */
    private void updateSellerStats(UUID sellerId) {
        Double avg = reviewRepository.getAverageRating(sellerId);
        Integer count = reviewRepository.countBySellerId(sellerId);

        SellerProfile profile = profileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setRating(avg != null ? avg.floatValue() : 0.0f);
        profile.setReviewCount(count != null ? count : 0);

        profileRepository.save(profile);
    }

    /**
     * Retrieves all reviews for a specific seller with pagination.
     */
    @Transactional(readOnly = true)
    public Page<SellerReviewResponseDTO> getReviewsBySeller(UUID sellerId, Pageable pageable) {
        return reviewRepository.findAllBySellerId(sellerId, pageable)
                .map(this::mapToDTO);
    }

    private SellerReviewResponseDTO mapToDTO(SellerReview r) {
        return new SellerReviewResponseDTO(
                r.getId(), r.getUserId(), r.getUserName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }
}