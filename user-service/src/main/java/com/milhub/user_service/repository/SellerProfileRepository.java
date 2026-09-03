package com.milhub.user_service.repository;

import com.milhub.user_service.entity.SellerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
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

    /**
     * Retrieves all seller profiles with their associated user eager-fetched in a single JOIN query.
     */
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT sp FROM SellerProfile sp")
    List<SellerProfile> findAllWithUser();
}