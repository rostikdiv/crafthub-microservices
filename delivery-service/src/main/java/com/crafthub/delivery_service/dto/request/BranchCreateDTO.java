package com.crafthub.delivery_service.dto.request;

public record BranchCreateDTO(
        String externalId,    // "ref-br-1"
        String branchNumber,  // "1"
        String name           // "Відділення №1..."
) {}