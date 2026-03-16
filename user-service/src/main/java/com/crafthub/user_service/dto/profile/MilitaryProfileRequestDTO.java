package com.crafthub.user_service.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for requesting the creation of a military profile.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MilitaryProfileRequestDTO {
        private String unitNumber;
        private String edrpou;
        private String commanderName;
        private String officialAddress;
}