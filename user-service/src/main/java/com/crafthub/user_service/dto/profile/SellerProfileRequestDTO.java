package com.crafthub.user_service.dto.profile;

public record SellerProfileRequestDTO(
        String companyName,
        String description,
        String taxId, // ЄДРПОУ або ІПН
        String logoUrl
) {}