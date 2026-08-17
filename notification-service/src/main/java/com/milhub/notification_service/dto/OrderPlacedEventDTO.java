package com.milhub.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing an order that has been successfully placed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPlacedEventDTO(
                UUID orderId,
                UUID userId,
                String userEmail,
                BigDecimal totalPrice,
                String productName,
                List<UUID> productIds) {
}