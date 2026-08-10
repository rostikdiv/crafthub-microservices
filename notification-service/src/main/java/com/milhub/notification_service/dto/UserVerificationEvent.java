package com.milhub.notification_service.dto;

import java.util.UUID;

/**
 * Event record representing a user verification result.
 */
public record UserVerificationEvent(
                UUID userId,
                String email,
                boolean isVerified,
                String reason // Admin comment or rejection reason
) {
}