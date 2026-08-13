package com.milhub.product_service.service;

import com.milhub.product_service.client.UserServiceClient;
import com.milhub.product_service.dto.SellerInfoDTO;
import com.milhub.product_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service responsible for validating product-related operations,
 * enforcing SOLID (Single Responsibility Principle) by separating validation
 * from core product logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductValidatorService {

    private final UserServiceClient userServiceClient;

    /**
     * Validates if the user is a valid seller and retrieves their information.
     *
     * @param userId the UUID of the user
     * @return SellerInfoDTO containing seller details
     * @throws BusinessException if the user service is unavailable or the user is not a valid seller
     */
    public SellerInfoDTO validateAndGetSellerInfo(UUID userId) {
        SellerInfoDTO sellerInfo;
        try {
            sellerInfo = userServiceClient.getSellerInfo(userId);
        } catch (Exception e) {
            log.error("Failed to fetch seller info for user {}: {}", userId, e.getMessage());
            throw new BusinessException("Unable to validate seller profile. User service is currently unavailable.");
        }

        if (sellerInfo == null || !Boolean.TRUE.equals(sellerInfo.isVerified())) {
            throw new BusinessException("Seller profile is not verified. Only verified sellers can publish or update products.");
        }

        return sellerInfo;
    }
}
