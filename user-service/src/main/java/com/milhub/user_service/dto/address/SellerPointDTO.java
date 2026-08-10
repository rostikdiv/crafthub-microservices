package com.milhub.user_service.dto.address;

import java.util.UUID;

/**
 * Data Transfer Object representing a seller's physical pickup point.
 */
public record SellerPointDTO(
                UUID id,
                String name,

                // Geographical details
                String cityRef,
                String cityName,
                String region,

                // Physical address details
                String streetName,
                String building,
                String apartment,
                String zipCode,

                // Contact information
                String phone,
                String instructions) {
}