package com.milhub.product_service.repository;

import com.milhub.product_service.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    /**
     * Finds only root reviews (where parent is NULL) for a specific product.
     */
    @Query("SELECT r FROM ProductReview r WHERE r.productId = :productId AND r.parent IS NULL")
    Page<ProductReview> findAllRootReviewsByProductId(@Param("productId") UUID productId, Pageable pageable);

    boolean existsByUserIdAndProductIdAndParentIsNull(UUID userId, UUID productId);

    Page<ProductReview> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Calculates average rating for root reviews of a product.
     */
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.productId = :productId AND r.parent IS NULL AND r.rating IS NOT NULL")
    Double getAverageRatingByProductId(@Param("productId") UUID productId);

    /**
     * Counts the number of root reviews with ratings for a product.
     */
    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.productId = :productId AND r.parent IS NULL AND r.rating IS NOT NULL")
    Long getReviewCountByProductId(@Param("productId") UUID productId);

    /**
     * Finds all replies for a collection of parent review IDs in a single batch
     * query.
     */
    List<ProductReview> findAllByParentIdInOrderByCreatedAtAsc(Collection<UUID> parentIds);
}