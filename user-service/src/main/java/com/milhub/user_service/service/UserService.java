package com.milhub.user_service.service;

import com.milhub.user_service.dto.auth.ChangePasswordRequestDTO;
import com.milhub.user_service.dto.user.UserUpdateDTO;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.milhub.user_service.dto.user.MilitaryProfileDTO;
import com.milhub.user_service.dto.seller.SellerProfileDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;

/**
 * Service for core user management operations, including profile mapping and
 * updates.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Fetches a user by ID or throws an exception if not found.
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Helper to retrieve the currently authenticated user based on the security
     * context.
     */
    private User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);
        return getUserById(userId);
    }

    /**
     * Retrieves detailed user information, including associated seller and military
     * profiles.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByIdWithProfiles(UUID userId) {
        User user = userRepository.findByIdWithProfiles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToResponseDTO(user);
    }

    /**
     * Maps user entity and its sub-profiles to a comprehensive Data Transfer
     * Object.
     */
    public UserResponseDTO mapToResponseDTO(User user) {
        SellerProfileDTO sellerDTO = null;
        if (user.getSellerProfile() != null) {
            var s = user.getSellerProfile();
            sellerDTO = SellerProfileDTO.builder()
                    .id(s.getId())
                    .companyName(s.getCompanyName())
                    .description(s.getDescription())
                    .logoUrl(s.getLogoUrl())
                    .taxId(s.getTaxId())
                    .rating(s.getRating())
                    .reviewCount(s.getReviewCount())
                    .build();
        }

        MilitaryProfileDTO militaryDTO = null;
        if (user.getMilitaryProfile() != null) {
            var m = user.getMilitaryProfile();
            militaryDTO = MilitaryProfileDTO.builder()
                    .id(m.getId())
                    .unitNumber(m.getUnitNumber())
                    .edrpou(m.getEdrpou())
                    .commanderName(m.getCommanderName())
                    .officialAddress(m.getOfficialAddress())
                    .build();
        }

        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .avatarUrl(user.getAvatarUrl())
                .sellerProfile(sellerDTO)
                .militaryProfile(militaryDTO)
                .build();
    }

    /**
     * Updates the basic profile details of the current user.
     */
    @Transactional
    public User updateCurrentUser(UserUpdateDTO dto) {
        User user = getCurrentUser();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        return userRepository.save(user);
    }

    /**
     * Validates and updates the current user's password.
     */
    @Transactional
    public void changePassword(ChangePasswordRequestDTO dto) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect current password");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmationPassword())) {
            throw new BusinessException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Promotes an existing user to ADMIN status.
     * Only an existing ADMIN should be able to call this.
     */
    @Transactional
    public User promoteUserToAdmin(UUID userId) {
        User user = getUserById(userId);
        user.setRole(com.milhub.user_service.entity.enums.Role.ADMIN);
        user.setIsVerified(true);
        return userRepository.save(user);
    }
}
