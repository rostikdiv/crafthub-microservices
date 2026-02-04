package com.crafthub.product_service.repository;

import com.crafthub.product_service.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    // Знаходимо тільки батьківські коментарі для конкретного товару
    @Query("SELECT r FROM ProductReview r WHERE r.productId = :productId AND r.parent IS NULL")
    Page<ProductReview> findAllRootReviewsByProductId(@Param("productId") UUID productId, Pageable pageable);

    boolean existsByUserIdAndProductIdAndParentIsNull(UUID userId, UUID productId);

    Page<ProductReview> findAllByUserId(UUID userId, Pageable pageable);

    // 1. Рахуємо середній рейтинг (повертає Double або null, якщо відгуків немає)
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId AND r.parent IS NULL AND r.rating IS NOT NULL")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);

    // 2. Рахуємо кількість відгуків з оцінками
    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.productId = :productId AND r.parent IS NULL AND r.rating IS NOT NULL")
    Long getReviewCountByProductId(@Param("productId") UUID productId);

}