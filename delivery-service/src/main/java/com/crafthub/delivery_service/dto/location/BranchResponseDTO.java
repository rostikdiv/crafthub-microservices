package com.crafthub.delivery_service.dto.location;

import java.util.UUID;

public record BranchResponseDTO(
        UUID id,
        String externalId,
        String branchNumber, // "1"
        String name          // "Відділення №1: вул. Городоцька..."
) {}