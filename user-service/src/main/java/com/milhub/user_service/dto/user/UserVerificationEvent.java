package com.milhub.user_service.dto.user;

import java.util.UUID;

/**
 * Event record representing a user verification result, to be consumed by other
 * services (e.g., Notification).
 */
public record UserVerificationEvent(
                UUID userId,
                String email,
                boolean isVerified,
                String reason // Admin comment or rejection reason
) {
}