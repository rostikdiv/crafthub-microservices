package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.SellerPublicProfileDTO;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.dto.address.SellerPointDTO; // New import
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.SellerProfileRepository;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public SellerPublicProfileDTO getSellerPublicProfile(UUID sellerId) {
        // Шукаємо профіль продавця за ID користувача
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + sellerId));

        User user = profile.getUser();

        java.util.List<SellerPointDTO> points = profile.getPickupPoints() == null ? new java.util.ArrayList<>()
                : profile.getPickupPoints().stream()
                        .map(p -> new SellerPointDTO(
                                p.getId(),
                                p.getName(),
                                p.getCityRef(),
                                p.getCityName(),
                                p.getRegion(),
                                p.getStreetName(),
                                p.getBuilding(),
                                p.getApartment(),
                                p.getZipCode(),
                                p.getPhone(),
                                p.getInstructions()))
                        .toList();

        return new SellerPublicProfileDTO(
                user.getId(),
                profile.getCompanyName(),
                profile.getDescription(),
                profile.getLogoUrl(),
                profile.getRating() != null ? profile.getRating() : 0.0f,
                profile.getReviewCount() != null ? profile.getReviewCount() : 0,
                user.getIsVerified(),
                user.getCreatedAt().toLocalDateTime(),
                points); // ✅ Pass mapped points
    }

    @Transactional
    public void incrementSales(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + sellerId));

        if (profile.getTotalSales() == null) {
            profile.setTotalSales(0);
        }
        profile.setTotalSales(profile.getTotalSales() + 1);
        sellerProfileRepository.save(profile);
    }
}