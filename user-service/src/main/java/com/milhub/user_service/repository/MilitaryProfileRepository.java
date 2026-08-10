package com.milhub.user_service.repository;

import com.milhub.user_service.entity.MilitaryProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for MilitaryProfile entity operations.
 */
public interface MilitaryProfileRepository extends JpaRepository<MilitaryProfile, UUID> {

    /**
     * Retrieves the military profile associated with a specific user ID.
     */
    Optional<MilitaryProfile> findByUserId(UUID userId);

    /**
     * Checks if a military profile already exists with the given EDRPOU code.
     */
    boolean existsByEdrpou(String edrpou);
}