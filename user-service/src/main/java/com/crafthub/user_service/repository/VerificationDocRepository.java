package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.VerificationDoc;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for VerificationDoc entity operations.
 */
public interface VerificationDocRepository extends JpaRepository<VerificationDoc, UUID> {

    /**
     * Retrieves all documents with a specific verification status.
     */
    List<VerificationDoc> findAllByStatus(VerificationStatus status);

    /**
     * Retrieves all documents associated with a specific user ID.
     */
    List<VerificationDoc> findAllByUserId(UUID userId);
}