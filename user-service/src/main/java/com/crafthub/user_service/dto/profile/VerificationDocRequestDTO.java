package com.crafthub.user_service.dto.profile;

import com.crafthub.user_service.entity.enums.DocumentType;

public record VerificationDocRequestDTO(
        DocumentType documentType,
        String docUrl // URL на S3 або посилання на Google Drive
) {}