package com.milhub.product_service.service;

import com.milhub.product_service.client.UserServiceClient;
import com.milhub.product_service.dto.SellerInfoDTO;
import com.milhub.product_service.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductValidatorServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ProductValidatorService productValidatorService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("validateAndGetSellerInfo: returns SellerInfoDTO when seller is verified")
    void validateAndGetSellerInfo_WhenVerified_ShouldReturnSellerInfo() {
        SellerInfoDTO info = new SellerInfoDTO(userId, "MilCorp", "http://logo.png", true);
        when(userServiceClient.getSellerInfo(userId)).thenReturn(info);

        SellerInfoDTO result = productValidatorService.validateAndGetSellerInfo(userId);

        assertThat(result).isNotNull();
        assertThat(result.companyName()).isEqualTo("MilCorp");
        assertThat(result.isVerified()).isTrue();
    }

    @Test
    @DisplayName("validateAndGetSellerInfo: throws BusinessException when seller is not verified")
    void validateAndGetSellerInfo_WhenNotVerified_ShouldThrowBusinessException() {
        SellerInfoDTO info = new SellerInfoDTO(userId, "MilCorp", "http://logo.png", false);
        when(userServiceClient.getSellerInfo(userId)).thenReturn(info);

        assertThatThrownBy(() -> productValidatorService.validateAndGetSellerInfo(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seller profile is not verified");
    }

    @Test
    @DisplayName("validateAndGetSellerInfo: throws BusinessException when seller info is null")
    void validateAndGetSellerInfo_WhenSellerNull_ShouldThrowBusinessException() {
        when(userServiceClient.getSellerInfo(userId)).thenReturn(null);

        assertThatThrownBy(() -> productValidatorService.validateAndGetSellerInfo(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seller profile is not verified");
    }

    @Test
    @DisplayName("validateAndGetSellerInfo: throws BusinessException when User Service client throws exception")
    void validateAndGetSellerInfo_WhenClientFails_ShouldThrowBusinessException() {
        when(userServiceClient.getSellerInfo(userId)).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> productValidatorService.validateAndGetSellerInfo(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unable to validate seller profile. User service is currently unavailable.");
    }
}
