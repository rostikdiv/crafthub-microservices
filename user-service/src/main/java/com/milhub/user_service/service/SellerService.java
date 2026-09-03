package com.milhub.user_service.service;

import com.milhub.user_service.dto.seller.SellerPublicProfileDTO;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.dto.address.SellerPointDTO;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.SellerProfileRepository;
import com.milhub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for handling seller-specific operations like profile retrieval and
 * sales tracking.
 */
@Service
@RequiredArgsConstructor
public class SellerService {

        private final SellerProfileRepository sellerProfileRepository;
        private final UserRepository userRepository;

        /**
         * Retrieves all seller public profiles (used for filter dropdowns).
         */
        @Transactional(readOnly = true)
        public List<SellerPublicProfileDTO> getAllSellers() {
                return sellerProfileRepository.findAllWithUser().stream()
                        .map(profile -> new SellerPublicProfileDTO(
                                profile.getUser().getId(),
                                profile.getCompanyName(),
                                profile.getDescription(),
                                profile.getLogoUrl(),
                                profile.getRating() != null ? profile.getRating() : 0.0f,
                                profile.getReviewCount() != null ? profile.getReviewCount() : 0,
                                profile.getUser().getIsVerified(),
                                profile.getUser().getCreatedAt().toLocalDateTime(),
                                java.util.Collections.emptyList()))
                        .sorted(java.util.Comparator.comparing(
                                s -> s.companyName() != null ? s.companyName() : ""))
                        .toList();
        }

        /**
         * Retrieves the public profile details for a seller, including their associated
         * pickup points.
         *
         * @param sellerId The ID of the seller (User ID).
         * @return A Data Transfer Object containing public profile information.
         */
        @Transactional(readOnly = true)
        public SellerPublicProfileDTO getSellerPublicProfile(UUID sellerId) {
                SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Seller profile not found for user: " + sellerId));

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
                                points);
        }

        /**
         * Increments the total sales count for a specified seller.
         */
        @Transactional
        public void incrementSales(UUID sellerId) {
                SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Seller profile not found for user: " + sellerId));

                if (profile.getTotalSales() == null) {
                        profile.setTotalSales(0);
                }
                profile.setTotalSales(profile.getTotalSales() + 1);
                sellerProfileRepository.save(profile);
        }

        /**
         * Retrieves the auto-confirm setting for a given seller.
         */
        @Transactional(readOnly = true)
        public Boolean getAutoConfirm(UUID sellerId) {
                return sellerProfileRepository.findByUserId(sellerId)
                        .map(SellerProfile::getAutoConfirmOrders)
                        .orElse(true);
        }
}