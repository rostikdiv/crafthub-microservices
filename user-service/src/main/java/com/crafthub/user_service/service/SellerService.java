package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.SellerPublicProfileDTO;
import com.crafthub.user_service.entity.SellerProfile;
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

        return new SellerPublicProfileDTO(
                user.getId(),
                profile.getCompanyName(),
                profile.getDescription(),
                profile.getLogoUrl(),
                profile.getRating() != null ? profile.getRating() : 0.0f,
                profile.getReviewCount() != null ? profile.getReviewCount() : 0, // ✅ ДОДАНО: Кількість відгуків
                user.getIsVerified(),
                user.getCreatedAt().toLocalDateTime()
        );
    }
}