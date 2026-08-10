package com.milhub.user_service.dto.admin;

import com.milhub.user_service.entity.enums.Role;
import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationRequestResponseDTO(
        UUID userId,
        String email,
        String fullName,
        Role role,
        String specificName, // Was companyOrUnitName
        LocalDateTime createdAt, // Was registeredAt
        long pendingDocsCount) {
}