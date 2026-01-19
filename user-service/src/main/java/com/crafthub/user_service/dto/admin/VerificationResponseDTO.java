package com.crafthub.user_service.dto.admin;

import com.crafthub.user_service.entity.enums.DocumentType;
import com.crafthub.user_service.entity.enums.VerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record VerificationResponseDTO(
        UUID id,                // ID самого документа (щоб його апрувнути)
        UUID userId,            // Чий документ
        DocumentType type,      // Тип (PASSPORT, MILITARY_ID...)
        String url,             // Посилання на фото
        VerificationStatus status,
        String rejectionReason, // Якщо відхилено
        LocalDateTime uploadedAt
) {}