package com.crafthub.user_service.repository;

import com.crafthub.user_service.entity.VerificationDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VerificationDocRepository extends JpaRepository<VerificationDoc, UUID> {
    List<VerificationDoc> findAllByUserId(UUID userId);
}