package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.MilitaryProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MilitaryProfileRepository extends JpaRepository<MilitaryProfile, UUID> {
    Optional<MilitaryProfile> findByUserId(UUID userId);
    boolean existsByEdrpou(String edrpou);
}