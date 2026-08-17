package com.milhub.order_service.dto.seller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SellerPublicProfileDTO(
        UUID userId,
        String companyName,
        Boolean isVerified) {
}
