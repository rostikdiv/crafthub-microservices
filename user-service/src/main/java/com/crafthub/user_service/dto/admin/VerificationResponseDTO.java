package com.crafthub.user_service.dto.admin;

import com.crafthub.user_service.entity.enums.DocumentType;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for verification document response details.
 */
public record VerificationResponseDTO(
        UUID id,
        UUID userId,
        DocumentType documentType,
        String docUrl,
        VerificationStatus status,
        LocalDateTime createdAt) {
}