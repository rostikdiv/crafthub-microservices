package com.milhub.user_service.repository;

import com.milhub.user_service.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SavedAddress entity operations.
 */
public interface SavedAddressRepository extends JpaRepository<SavedAddress, UUID> {

    /**
     * Retrieves all saved addresses linked to a specific user.
     */
    List<SavedAddress> findAllByUserId(UUID userId);
}