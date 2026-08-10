package com.milhub.user_service.dto.user;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * Data Transfer Object for military profile details.
 */
@Data
@Builder
public class MilitaryProfileDTO {
    private UUID id;
    private String unitNumber;
    private String edrpou;
    private String commanderName;
    private String officialAddress;
}
