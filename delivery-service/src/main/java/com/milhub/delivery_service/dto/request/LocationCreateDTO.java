package com.milhub.delivery_service.dto.request;

import com.milhub.delivery_service.entity.DeliveryProvider;
import java.util.List;

/**
 * Data Transfer Object for creating a new location.
 */
public record LocationCreateDTO(
                DeliveryProvider provider, // e.g., NOVA_POSHTA
                String externalId, // Carrier-specific reference
                String nameUkr, // Ukrainian city name
                String region, // Region name
                List<BranchCreateDTO> branches // Nested list of branches to import
) {
}