package com.crafthub.delivery_service.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for carrying error response details.
 */
public record ErrorResponse(
                LocalDateTime timestamp,
                int status,
                String error,
                String message,
                String path,
                Map<String, String> validationErrors) {
}