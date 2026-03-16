package com.crafthub.delivery_service.dto.external;

import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.entity.DeliveryType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Data Transfer Object for detailed delivery information, including provider,
 * address, and pickup details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryDetailsDTO(
                DeliveryProvider provider,
                DeliveryType type,
                String cityRef,
                String cityName,
                String region,
                String branchRef,
                String branchName,
                String street,
                String building,
                String apartment,
                String zipCode,
                UUID sellerPointId,
                String pickupAddress,
                String pickupInstructions) {
}