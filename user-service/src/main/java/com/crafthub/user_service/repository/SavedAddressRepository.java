package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SavedAddressRepository extends JpaRepository<SavedAddress, UUID> {
    List<SavedAddress> findAllByUserId(UUID userId);
}