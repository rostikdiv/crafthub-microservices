package com.milhub.user_service.dto.seller;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * Data Transfer Object for detailed seller profile information.
 */
@Data
@Builder
public class SellerProfileDTO {
    private UUID id;
    private String companyName;
    private String description;
    private String logoUrl;
    private String taxId;
    private Float rating;
    private Integer reviewCount;
}
