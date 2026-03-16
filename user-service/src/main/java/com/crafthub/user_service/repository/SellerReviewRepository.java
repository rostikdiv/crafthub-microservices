package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.SellerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository interface for SellerReview entity operations.
 */
public interface SellerReviewRepository extends JpaRepository<SellerReview, UUID> {

    /**
     * Retrieves all reviews for a specific seller with pagination support.
     */
    Page<SellerReview> findAllBySellerId(UUID sellerId, Pageable pageable);

    /**
     * Calculates the average rating for a specified seller.
     */
    @Query("SELECT AVG(r.rating) FROM SellerReview r WHERE r.sellerId = :sellerId")
    Double getAverageRating(@Param("sellerId") UUID sellerId);

    /**
     * Counts the total number of reviews for a specified seller.
     */
    @Query("SELECT COUNT(r) FROM SellerReview r WHERE r.sellerId = :sellerId")
    Integer countBySellerId(@Param("sellerId") UUID sellerId);
}