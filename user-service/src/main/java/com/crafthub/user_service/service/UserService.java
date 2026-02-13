package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.ChangePasswordRequestDTO;
import com.crafthub.user_service.dto.UserUpdateDTO;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import com.crafthub.user_service.dto.MilitaryProfileDTO;
import com.crafthub.user_service.dto.SellerProfileDTO;
import com.crafthub.user_service.dto.UserResponseDTO;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // --- HELPER Methods ---

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);
        return getUserById(userId);
    }

    // --- PUBLIC API ---

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByIdWithProfiles(UUID userId) {
        User user = getUserById(userId);
        return mapToResponseDTO(user);
    }

    private UserResponseDTO mapToResponseDTO(User user) {
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

    @Transactional
    public User updateCurrentUser(UserUpdateDTO dto) {
        User user = getCurrentUser();

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        return userRepository.save(user);
    }

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
}
