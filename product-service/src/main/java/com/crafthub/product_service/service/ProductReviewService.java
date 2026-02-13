package com.crafthub.product_service.service;

import com.crafthub.product_service.client.OrderServiceClient;
import com.crafthub.product_service.client.UserServiceClient;
import com.crafthub.product_service.dto.review.ProductReviewRequestDTO;
import com.crafthub.product_service.dto.review.ProductReviewResponseDTO;
import com.crafthub.product_service.dto.review.UserReviewHistoryDTO;
import com.crafthub.product_service.entity.ProductReview;
import com.crafthub.product_service.exception.ResourceNotFoundException;
import com.crafthub.product_service.repository.ProductRepository;
import com.crafthub.product_service.repository.ProductReviewRepository;
import com.crafthub.product_service.security.UserContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderServiceIntegration orderServiceIntegration;
    private final UserContextService userContext;
    private final UserServiceClient userServiceClient; // Якщо треба дістати ім'я

    @Transactional
    public ProductReviewResponseDTO addReview(ProductReviewRequestDTO request) {
        UUID userId = userContext.getUserId();
        // Можна дістати ім'я з токена (якщо там є) або UserServiceClient
        String userName = "User " + userId.toString().substring(0, 5);

        // 1. Перевірка: Це кореневий відгук чи відповідь?
        ProductReview parent = null;
        if (request.parentId() != null) {
            parent = reviewRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent review not found"));

            // Валідація: не можна відповідати на коментар іншого товару
            if (!parent.getProductId().equals(request.productId())) {
                throw new IllegalArgumentException("Product ID mismatch");
            }
        }

        // 2. Перевірка "Verified Purchase" (тільки для автора, не залежить від батька)
        // Ми перевіряємо, чи цей КОНКРЕТНИЙ юзер купував товар
        Boolean isVerified = orderServiceIntegration.checkPurchase(request.productId());
        if (isVerified == null)
            isVerified = false;

        // 3. Створення
        ProductReview review = ProductReview.builder()
                .productId(request.productId())
                .userId(userId)
                .userName(userName)
                .rating(request.rating()) // Для відповіді може бути null, якщо фронт не шле
                .comment(request.comment())
                .isVerifiedPurchase(isVerified)
                .parent(parent)
                .build();

        reviewRepository.save(review);

        // 4. Оновлення рейтингу товару (ТІЛЬКИ якщо це кореневий відгук з оцінкою)
        if (parent == null && request.rating() != null) {
            updateProductRating(request.productId());
        }

        return mapToDTO(review);
    }

    public Page<ProductReviewResponseDTO> getReviewsByProduct(UUID productId, Pageable pageable) {
        // Беремо тільки верхній рівень, рекурсія підтягне дітей у mapToDTO
        return reviewRepository.findAllRootReviewsByProductId(productId, pageable)
                .map(this::mapToDTO);
    }

    private void updateProductRating(UUID productId) {
        // 1. Отримуємо агреговані дані з БД
        Double averageRating = reviewRepository.getAverageRatingByProductId(productId);
        Long reviewCount = reviewRepository.getReviewCountByProductId(productId);

        // 2. Обробка null (якщо всі відгуки видалили або їх ще немає)
        double newRating = (averageRating != null) ? averageRating : 0.0;
        int newCount = (reviewCount != null) ? reviewCount.intValue() : 0;

        // 3. Округлення до 1 знаку після коми (опціонально, але гарно для UI)
        // Наприклад: 4.6666 -> 4.7
        newRating = Math.round(newRating * 10.0) / 10.0;

        // 4. Оновлення товару
        // Використовуємо var для скорочення або Product product
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        product.setAverageRating(newRating);
        product.setReviewCount(newCount);

        productRepository.save(product);

        // log.info("Updated rating for product {}: {} stars ({} reviews)", productId,
        // newRating, newCount);
    }

    @Transactional(readOnly = true)
    public Page<UserReviewHistoryDTO> getUserReviewHistory(Pageable pageable) {
        UUID userId = userContext.getUserId();

        return reviewRepository.findAllByUserId(userId, pageable)
                .map(this::mapToHistoryDTO);
    }

    private UserReviewHistoryDTO mapToHistoryDTO(ProductReview review) {
        // Отримуємо товар (щоб показати назву і картинку)
        // Краще використовувати кешування або batch-fetching, щоб не робити запит на
        // кожен рядок
        var product = productRepository.findById(review.getProductId()).orElse(null);
        String productName = (product != null) ? product.getName() : "Unknown Product";
        String productImg = (product != null) ? product.getPreviewImageUrl() : null;

        // Логіка визначення контексту відповіді
        boolean isReply = review.getParent() != null;
        String replyToUser = null;
        String replyToText = null;

        if (isReply) {
            replyToUser = review.getParent().getUserName();
            // Обрізаємо текст батька, якщо він довгий
            String parentText = review.getParent().getComment();
            replyToText = parentText.length() > 50 ? parentText.substring(0, 50) + "..." : parentText;
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

    // Рекурсивний маппер
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
                review.getReplies().stream().map(this::mapToDTO).collect(Collectors.toList()));
    }
}