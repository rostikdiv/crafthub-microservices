package com.milhub.user_service.dto.address;

import com.milhub.user_service.entity.enums.DeliveryProvider;
import com.milhub.user_service.entity.enums.DeliveryType;
import java.util.UUID;

/**
 * Data Transfer Object representing a user's delivery address.
 */
public record AddressDTO(
                UUID id,
                String title,
                DeliveryProvider provider,
                DeliveryType deliveryType,
                String cityRef,
                String cityName,
                String region,
                String branchRef,
                String branchName,
                String streetName,
                String building,
                String apartment,
                String zipCode) {
}