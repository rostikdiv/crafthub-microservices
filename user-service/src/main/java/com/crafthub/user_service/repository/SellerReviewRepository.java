package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.SellerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SellerReviewRepository extends JpaRepository<SellerReview, UUID> {

    // Отримати всі відгуки для конкретного продавця
    Page<SellerReview> findAllBySellerId(UUID sellerId, Pageable pageable);

    // 📊 АГРЕГАЦІЯ: Рахуємо середній рейтинг і кількість
    @Query("SELECT AVG(r.rating) FROM SellerReview r WHERE r.sellerId = :sellerId")
    Double getAverageRating(@Param("sellerId") UUID sellerId);

    @Query("SELECT COUNT(r) FROM SellerReview r WHERE r.sellerId = :sellerId")
    Integer countBySellerId(@Param("sellerId") UUID sellerId);
}