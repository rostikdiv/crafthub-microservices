package com.crafthub.user_service.dto.admin;

import com.crafthub.user_service.entity.enums.Role;
import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationRequestResponseDTO(
        UUID userId,
        String email,
        String fullName,
        Role role,                // Поточна роль (наприклад, BUYER)
        String companyOrUnitName, // Назва магазину або номер частини (з профілю)
        LocalDateTime registeredAt,
        long pendingDocsCount     // Скільки документів ще не перевірено
) {}