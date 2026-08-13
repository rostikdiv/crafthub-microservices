package com.milhub.order_service.dto.seller;

import java.util.UUID;

public record SellerPublicProfileDTO(
        UUID userId,
        String companyName,
        Boolean isVerified) {
}
