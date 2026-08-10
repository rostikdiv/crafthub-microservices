package com.milhub.order_service.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API error response DTO.
 */
public record ErrorResponse(
                LocalDateTime timestamp,
                int status,
                String error,
                String message,
                String path,
                Map<String, String> errors) {
}