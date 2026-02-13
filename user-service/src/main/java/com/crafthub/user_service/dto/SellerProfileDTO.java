package com.crafthub.user_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

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
