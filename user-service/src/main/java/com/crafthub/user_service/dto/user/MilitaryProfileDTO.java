package com.crafthub.user_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MilitaryProfileDTO {
    private UUID id;
    private String unitNumber;
    private String edrpou;
    private String commanderName;
    private String officialAddress;
}
