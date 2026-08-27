package com.milhub.order_service.dto.delivery;

import com.milhub.order_service.entity.enums.DeliveryProvider;
import com.milhub.order_service.entity.enums.DeliveryType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.UUID;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryDetailsDTO(
        // 1. Main toggles
        DeliveryProvider provider,
        DeliveryType type,

        // 2. Recipient (Customer / Contact info)
        String recipientName,
        String recipientPhone,
        String recipientEmail,

        // 3. Geography (Common for all delivery types except self-pickup without location reference)
        String cityRef, // "ref-lviv-np"
        String cityName, // "Lviv"
        String region, // "Lviv region"

        // 3. When BRANCH
        String branchRef, // "ref-br-1"
        String branchName, // "Branch #1: ..."

        // 4. When COURIER (Address delivery)
        String street,
        String building,
        String apartment,
        String zipCode,

        // 5. When SELF_PICKUP
        UUID sellerPointId, // Point ID in User Service
        String pickupAddress, // Text address (snapshot)
        String pickupInstructions) { // "Code 359"
}