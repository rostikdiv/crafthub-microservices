package com.crafthub.user_service.dto;

import com.crafthub.user_service.entity.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponseDTO {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private Boolean isVerified;
    private String avatarUrl;

    private SellerProfileDTO sellerProfile;
    private MilitaryProfileDTO militaryProfile;
}
