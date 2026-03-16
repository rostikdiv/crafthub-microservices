package com.crafthub.user_service.dto.profile;

import com.crafthub.user_service.entity.enums.DocumentType;

/**
 * Data Transfer Object for uploading a new verification document.
 */
public record VerificationDocRequestDTO(
                DocumentType documentType,
                String docUrl // URL to S3 storage or other document provider
) {
}