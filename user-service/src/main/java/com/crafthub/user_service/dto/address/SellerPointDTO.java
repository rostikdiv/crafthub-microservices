package com.crafthub.user_service.dto.address;

import java.util.UUID;

public record SellerPointDTO(
        UUID id,
        String name,        // "Майстерня на Подолі"

        // --- Географія (Уніфікована з AddressDTO) ---
        String cityRef,     // "ref-lviv-np" (або інший ID)
        String cityName,    // "Львів"
        String region,      // "Львівська область"

        // --- Адресна частина ---
        String streetName,  // "вул. Спаська"
        String building,    // "10"
        String apartment,   // "офіс 5" (опціонально)
        String zipCode,     // "04070"

        // --- Контакти ---
        String phone,
        String instructions // "Код домофону 123"
) {}