package com.milhub.delivery_service.dto.location;

import java.util.UUID;

/**
 * Data Transfer Object for location response details.
 */
public record LocationResponseDTO(
                UUID id, // Internal unique identifier
                String externalId, // Carrier-specific reference ID
                String name, // Location name (e.g., "Lviv")
                String region // Region/Province name
) {
}