package com.crafthub.order_service.dto.delivery;

import com.crafthub.order_service.entity.enums.DeliveryProvider;
import com.crafthub.order_service.entity.enums.DeliveryType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.UUID;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliveryDetailsDTO(
        // 1. Головні перемикачі
        DeliveryProvider provider,
        DeliveryType type,

        // 2. Географія (Спільне для всіх крім чистого самовивозу без прив'язки)
        String cityRef,     // "ref-lviv-np"
        String cityName,    // "Львів"
        String region,      // "Львівська область"

        // 3. Якщо BRANCH (Відділення)
        String branchRef,   // "ref-br-1"
        String branchName,  // "Відділення №1: ..."

        // 4. Якщо COURIER (Адресна)
        String street,
        String building,
        String apartment,
        String zipCode,

        // 5. Якщо SELF_PICKUP (Самовивіз)
        UUID sellerPointId,        // ID точки в User Service
        String pickupAddress,      // Текстова адреса (snapshot)
        String pickupInstructions  // "Код 359"
) {}