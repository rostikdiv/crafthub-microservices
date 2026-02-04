package com.crafthub.user_service.dto.admin;

import com.crafthub.user_service.entity.enums.DocumentType;
import com.crafthub.user_service.entity.enums.VerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationResponseDTO(
        UUID id,
        UUID userId,
        DocumentType type,
        String url,
        VerificationStatus status,
        LocalDateTime uploadedAt // Заповнюємо з doc.getCreatedAt()
) {}