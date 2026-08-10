package com.milhub.user_service.dto.user;

import com.milhub.user_service.entity.enums.Role;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

/**
 * Data Transfer Object for user response details, including profiles.
 */
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

    private com.milhub.user_service.dto.seller.SellerProfileDTO sellerProfile;
    private MilitaryProfileDTO militaryProfile;
}
