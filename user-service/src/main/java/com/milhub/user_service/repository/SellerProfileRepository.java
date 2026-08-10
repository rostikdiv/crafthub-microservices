package com.milhub.user_service.repository;

import com.milhub.user_service.entity.SellerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for SellerProfile entity operations.
 */
public interface SellerProfileRepository extends JpaRepository<SellerProfile, UUID> {

    /**
     * Retrieves the seller profile associated with a specific user ID.
     */
    Optional<SellerProfile> findByUserId(UUID userId);

    /**
     * Checks if a seller profile already exists with the given Tax ID.
     */
    boolean existsByTaxId(String taxId);
}