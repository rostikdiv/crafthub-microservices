package com.crafthub.notification_service.dto;

import java.util.UUID;

public record UserVerificationEvent(
        UUID userId,
        String email,
        boolean isVerified,
        String reason // Коментар адміна (причина відмови або вітання)
) {}