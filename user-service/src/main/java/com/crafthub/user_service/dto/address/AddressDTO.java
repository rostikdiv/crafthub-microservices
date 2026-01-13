package com.crafthub.user_service.dto.address;

import com.crafthub.user_service.entity.enums.DeliveryProvider;
import com.crafthub.user_service.entity.enums.DeliveryType;
import java.util.UUID;

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
        String zipCode
) {}