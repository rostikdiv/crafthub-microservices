package com.milhub.user_service.service;

import com.milhub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.milhub.user_service.dto.profile.SellerProfileRequestDTO;
import com.milhub.user_service.dto.profile.VerificationDocRequestDTO;
import com.milhub.user_service.entity.*;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing specialized user profiles (Seller, Military) and
 * supporting documents.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final MilitaryProfileRepository militaryProfileRepository;
    private final VerificationDocRepository verificationDocRepository;

    /**
     * Extracts the current authenticated user from the security context.
     * User ID is typically provided as a principal string.
     */
    private User getCurrentUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Creates a new seller profile for the current user.
     * Throws an exception if a profile already exists for this user.
     */
    @Transactional
    public void createSellerProfile(SellerProfileRequestDTO dto) {
        User user = getCurrentUser();

        if (sellerProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException("Seller profile already created. Please wait for verification.");
        }

        SellerProfile profile = SellerProfile.builder()
                .user(user)
                .companyName(dto.getCompanyName())
                .description(dto.getDescription())
                .taxId(dto.getTaxId())
                .logoUrl(dto.getLogoUrl())
                .rating(0.0f)
                .autoConfirmOrders(dto.getAutoConfirmOrders() != null ? dto.getAutoConfirmOrders() : true)
                .build();

        sellerProfileRepository.save(profile);
    }

    /**
     * Creates a new military profile for the current user.
     * Throws an exception if a military profile already exists.
     */
    @Transactional
    public void createMilitaryProfile(MilitaryProfileRequestDTO dto) {
        User user = getCurrentUser();

        if (militaryProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException("Military profile already created. Please wait for verification.");
        }

        MilitaryProfile profile = MilitaryProfile.builder()
                .user(user)
                .unitNumber(dto.getUnitNumber())
                .edrpou(dto.getEdrpou())
                .commanderName(dto.getCommanderName())
                .officialAddress(dto.getOfficialAddress())
                .build();

        militaryProfileRepository.save(profile);
    }

    /**
     * Updates specific fields of an existing seller profile.
     */
    @Transactional
    public void updateSellerProfile(SellerProfileRequestDTO dto) {
        User user = getCurrentUser();
        SellerProfile profile = user.getSellerProfile();

        if (profile == null) {
            throw new ResourceNotFoundException("Seller profile not created yet");
        }

        profile.setCompanyName(dto.getCompanyName());
        profile.setDescription(dto.getDescription());
        profile.setLogoUrl(dto.getLogoUrl());
        if (dto.getAutoConfirmOrders() != null) {
            profile.setAutoConfirmOrders(dto.getAutoConfirmOrders());
        }
        // Tax ID is usually immutable without further verification

        sellerProfileRepository.save(profile);
    }

    /**
     * Updates an existing military profile and resets verification.
     */
    @Transactional
    public void updateMilitaryProfile(MilitaryProfileRequestDTO dto) {
        User user = getCurrentUser();
        MilitaryProfile profile = user.getMilitaryProfile();

        if (profile == null) {
            throw new ResourceNotFoundException("Military profile not created yet");
        }

        boolean hasChanges = false;

        if (!java.util.Objects.equals(profile.getUnitNumber(), dto.getUnitNumber())) {
            profile.setUnitNumber(dto.getUnitNumber());
            hasChanges = true;
        }
        if (!java.util.Objects.equals(profile.getEdrpou(), dto.getEdrpou())) {
            profile.setEdrpou(dto.getEdrpou());
            hasChanges = true;
        }
        if (!java.util.Objects.equals(profile.getCommanderName(), dto.getCommanderName())) {
            profile.setCommanderName(dto.getCommanderName());
            hasChanges = true;
        }
        if (!java.util.Objects.equals(profile.getOfficialAddress(), dto.getOfficialAddress())) {
            profile.setOfficialAddress(dto.getOfficialAddress());
            hasChanges = true;
        }

        if (hasChanges) {
            militaryProfileRepository.save(profile);

            // Reset verification and downgrade role since the profile data changed
            user.setIsVerified(false);
            user.setRole(Role.BUYER);
            userRepository.save(user);
        }
    }

    /**
     * Adds a verification document (e.g., ID, certificate) linked to the current
     * user.
     */
    @Transactional
    public void addVerificationDocument(VerificationDocRequestDTO dto) {
        User user = getCurrentUser();

        VerificationDoc doc = VerificationDoc.builder()
                .user(user)
                .documentType(dto.documentType())
                .docUrl(dto.docUrl())
                .status(VerificationStatus.PENDING)
                .build();

        verificationDocRepository.save(doc);
    }
}