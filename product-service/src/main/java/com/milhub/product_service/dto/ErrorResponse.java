package com.milhub.product_service.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure for the application.
 */
public record ErrorResponse(
                LocalDateTime timestamp,
                int status,
                String error,
                String message,
                String path,
                Map<String, String> validationErrors) {
}