package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.crafthub.user_service.dto.profile.SellerProfileRequestDTO;
import com.crafthub.user_service.dto.profile.VerificationDocRequestDTO;
import com.crafthub.user_service.entity.*;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.entity.enums.VerificationStatus;
import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final MilitaryProfileRepository militaryProfileRepository;
    private final VerificationDocRepository verificationDocRepository;

    private User getCurrentUser() {
        // Оскільки в SecurityContext тепер лежить ID (String), а не UserDetails
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // --- 1. Створення профілю Продавця ---
    @Transactional
    public void createSellerProfile(SellerProfileRequestDTO dto) {
        User user = getCurrentUser();

        if (sellerProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException("Seller profile already created. Please wait for verification.");
        }

        SellerProfile profile = SellerProfile.builder()
                .user(user)
                .companyName(dto.companyName())
                .description(dto.description())
                .taxId(dto.taxId())
                .logoUrl(dto.logoUrl())
                .rating(0.0f)
                .build();

        sellerProfileRepository.save(profile);
    }

    // --- 2. Створення профілю Військового ---
    @Transactional
    public void createMilitaryProfile(MilitaryProfileRequestDTO dto) {
        User user = getCurrentUser();

        if (militaryProfileRepository.findByUserId(user.getId()).isPresent()) {
            throw new BusinessException("Military profile already created. Please wait for verification.");
        }

        MilitaryProfile profile = MilitaryProfile.builder()
                .user(user)
                .unitNumber(dto.unitNumber())
                .edrpou(dto.edrpou())
                .commanderName(dto.commanderName())
                .officialAddress(dto.officialAddress())
                .build();

        militaryProfileRepository.save(profile);
    }

    @Transactional
    public void updateSellerProfile(SellerProfileRequestDTO dto) {
        User user = getCurrentUser();
        SellerProfile profile = user.getSellerProfile();

        if (profile == null) {
            throw new ResourceNotFoundException("Seller profile not created yet");
        }

        // Оновлюємо дозволені поля
        profile.setCompanyName(dto.companyName());
        profile.setDescription(dto.description());
        profile.setLogoUrl(dto.logoUrl());
        // taxId зазвичай не змінюють просто так, бо це вимагає нової верифікації

        sellerProfileRepository.save(profile);
    }

    // --- 4. Завантаження документів верифікації ---
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