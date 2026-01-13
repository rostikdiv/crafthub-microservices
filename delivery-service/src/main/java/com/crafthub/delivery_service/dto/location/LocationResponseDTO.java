package com.crafthub.delivery_service.dto.location;

import java.util.UUID;

public record LocationResponseDTO(
        UUID id,            // Наш внутрішній ID
        String externalId,  // Ref перевізника
        String name,        // "Львів"
        String region       // "Львівська область"
) {}