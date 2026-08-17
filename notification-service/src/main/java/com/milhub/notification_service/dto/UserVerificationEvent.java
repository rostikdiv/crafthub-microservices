package com.milhub.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Event record representing a user verification result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserVerificationEvent(
                UUID userId,
                String email,
                boolean isVerified,
                String reason // Admin comment or rejection reason
) {
}