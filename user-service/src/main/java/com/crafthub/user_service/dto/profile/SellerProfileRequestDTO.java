package com.crafthub.user_service.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for requesting the creation of a seller profile.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SellerProfileRequestDTO {
        private String companyName;
        private String description;
        private String logoUrl;
        private String taxId;
        private Boolean autoConfirmOrders;
}