package com.crafthub.user_service.dto.admin;

import com.crafthub.user_service.entity.enums.DocumentType;
import com.crafthub.user_service.entity.enums.VerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationResponseDTO(
                UUID id,
                UUID userId,
                DocumentType documentType, // Was type
                String docUrl, // Was url
                VerificationStatus status,
                LocalDateTime createdAt // Was uploadedAt
) {
}