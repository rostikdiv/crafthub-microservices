package com.milhub.user_service.repository;

import com.milhub.user_service.entity.SellerPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SellerPoint entity operations.
 */
public interface SellerPointRepository extends JpaRepository<SellerPoint, UUID> {

    /**
     * Retrieves all pickup points associated with a specific seller profile.
     */
    List<SellerPoint> findAllBySellerProfileId(UUID sellerProfileId);
}