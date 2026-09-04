package com.milhub.user_service.service;

import com.milhub.user_service.dto.seller.SellerPublicProfileDTO;
import com.milhub.user_service.entity.SellerPoint;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerServiceTest {

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @InjectMocks
    private SellerService sellerService;

    private UUID sellerId;
    private User sellerUser;
    private SellerProfile sellerProfile;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        sellerUser = User.builder()
                .id(sellerId)
                .email("seller@milhub.ua")
                .isVerified(true)
                .createdAt(Timestamp.from(Instant.now()))
                .build();

        sellerProfile = SellerProfile.builder()
                .id(UUID.randomUUID())
                .user(sellerUser)
                .companyName("Alpha Tactical")
                .description("Quality gear")
                .logoUrl("http://logo.png")
                .rating(4.5f)
                .reviewCount(15)
                .totalSales(100)
                .autoConfirmOrders(true)
                .pickupPoints(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should return all sellers sorted by company name")
    void getAllSellers_ShouldReturnSortedList() {
        SellerProfile p2 = SellerProfile.builder()
                .user(sellerUser)
                .companyName("Bravo Gear")
                .build();

        when(sellerProfileRepository.findAllWithUser()).thenReturn(List.of(p2, sellerProfile));

        List<SellerPublicProfileDTO> result = sellerService.getAllSellers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).companyName()).isEqualTo("Alpha Tactical");
        assertThat(result.get(1).companyName()).isEqualTo("Bravo Gear");
    }

    @Test
    @DisplayName("Should return seller public profile with pickup points")
    void getSellerPublicProfile_WhenFound_ShouldReturnDTO() {
        SellerPoint point = SellerPoint.builder()
                .id(UUID.randomUUID())
                .name("Main Hub")
                .cityName("Kyiv")
                .streetName("Khreshchatyk")
                .building("1")
                .build();
        sellerProfile.getPickupPoints().add(point);

        when(sellerProfileRepository.findByUserId(sellerId)).thenReturn(Optional.of(sellerProfile));

        SellerPublicProfileDTO dto = sellerService.getSellerPublicProfile(sellerId);

        assertThat(dto).isNotNull();
        assertThat(dto.companyName()).isEqualTo("Alpha Tactical");
        assertThat(dto.pickupPoints()).hasSize(1);
        assertThat(dto.pickupPoints().get(0).name()).isEqualTo("Main Hub");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when seller profile not found")
    void getSellerPublicProfile_WhenNotFound_ShouldThrowException() {
        when(sellerProfileRepository.findByUserId(sellerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sellerService.getSellerPublicProfile(sellerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Seller profile not found for user: " + sellerId);
    }

    @Test
    @DisplayName("Should increment seller total sales count")
    void incrementSales_ShouldIncrementCountAndSave() {
        when(sellerProfileRepository.findByUserId(sellerId)).thenReturn(Optional.of(sellerProfile));

        sellerService.incrementSales(sellerId);

        assertThat(sellerProfile.getTotalSales()).isEqualTo(101);
        verify(sellerProfileRepository).save(sellerProfile);
    }

    @Test
    @DisplayName("Should return autoConfirm flag or default to true")
    void getAutoConfirm_ShouldReturnFlag() {
        when(sellerProfileRepository.findByUserId(sellerId)).thenReturn(Optional.of(sellerProfile));

        Boolean autoConfirm = sellerService.getAutoConfirm(sellerId);

        assertThat(autoConfirm).isTrue();
    }
}
