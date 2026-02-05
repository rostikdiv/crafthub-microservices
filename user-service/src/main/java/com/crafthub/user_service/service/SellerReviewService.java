package com.crafthub.user_service.service;

import com.crafthub.user_service.client.OrderServiceClient;
import com.crafthub.user_service.dto.review.SellerReviewRequestDTO; // Створіть цей DTO (rating, comment, sellerId)
import com.crafthub.user_service.dto.review.SellerReviewResponseDTO; // Створіть DTO для відповіді
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.entity.SellerReview;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.SellerProfileRepository;
import com.crafthub.user_service.repository.SellerReviewRepository;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerReviewService {

    private final SellerReviewRepository reviewRepository;
    private final SellerProfileRepository profileRepository;
    private final OrderServiceIntegration orderServiceIntegration;
    private final UserRepository userRepository;

    @Transactional
    public SellerReviewResponseDTO addReview(SellerReviewRequestDTO request) {
        // 1. Отримуємо поточного юзера
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 2. Не можна оцінювати самого себе
        if (userId.equals(request.sellerId())) {
            throw new BusinessException("You cannot review yourself");
        }

        // 3. ПЕРЕВІРКА ЧЕРЕЗ ORDER SERVICE
        Boolean hasBought = orderServiceIntegration.checkSellerPurchase(userId, request.sellerId());
        if (!Boolean.TRUE.equals(hasBought)) {
            throw new BusinessException("You can only review sellers you have purchased from (delivered orders).");
        }

        // 4. Зберігаємо відгук
        SellerReview review = SellerReview.builder()
                .sellerId(request.sellerId())
                .userId(userId)
                .userName(user.getFirstName() + " " + user.getLastName()) // Кешуємо ім'я
                .rating(request.rating())
                .comment(request.comment())
                .build();

        reviewRepository.save(review);

        // 5. 🔥 МИТТЄВЕ ОНОВЛЕННЯ РЕЙТИНГУ ПРОДАВЦЯ
        updateSellerStats(request.sellerId());

        return mapToDTO(review);
    }

    // Приватний метод для перерахунку статистики
    private void updateSellerStats(UUID sellerId) {
        Double avg = reviewRepository.getAverageRating(sellerId);
        Integer count = reviewRepository.countBySellerId(sellerId);

        SellerProfile profile = profileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setRating(avg != null ? avg.floatValue() : 0.0f);
        profile.setReviewCount(count != null ? count : 0);

        profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Page<SellerReviewResponseDTO> getReviewsBySeller(UUID sellerId, Pageable pageable) {
        return reviewRepository.findAllBySellerId(sellerId, pageable)
                .map(this::mapToDTO);
    }

    private SellerReviewResponseDTO mapToDTO(SellerReview r) {
        // Повертаємо DTO
        return new SellerReviewResponseDTO(
                r.getId(), r.getUserId(), r.getUserName(), r.getRating(), r.getComment(), r.getCreatedAt()
        );
    }
}