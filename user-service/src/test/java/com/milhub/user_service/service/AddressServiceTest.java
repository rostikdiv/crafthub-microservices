package com.milhub.user_service.service;

import com.milhub.user_service.dto.address.AddressDTO;
import com.milhub.user_service.dto.address.SellerPointDTO;
import com.milhub.user_service.entity.SavedAddress;
import com.milhub.user_service.entity.SellerPoint;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.DeliveryProvider;
import com.milhub.user_service.entity.enums.DeliveryType;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.repository.SavedAddressRepository;
import com.milhub.user_service.repository.SellerPointRepository;
import com.milhub.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private SavedAddressRepository addressRepository;

    @Mock
    private SellerPointRepository sellerPointRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressService addressService;

    private UUID userId;
    private User currentUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        currentUser = User.builder()
                .id(userId)
                .email("buyer@milhub.ua")
                .firstName("Dmytro")
                .lastName("Kotsiubailo")
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should save new buyer delivery address successfully")
    void saveAddress_Success_ShouldSaveAndReturnDTO() {
        AddressDTO inputDto = new AddressDTO(
                null, "Home Nova Poshta", DeliveryProvider.NOVA_POSHTA, DeliveryType.BRANCH,
                "city-ref-1", "Kyiv", "Kyiv Oblast",
                "branch-ref-1", "Branch #12",
                null, null, null, null
        );

        UUID addressId = UUID.randomUUID();
        SavedAddress savedAddress = SavedAddress.builder()
                .id(addressId)
                .user(currentUser)
                .title("Home Nova Poshta")
                .provider(DeliveryProvider.NOVA_POSHTA)
                .deliveryType(DeliveryType.BRANCH)
                .cityName("Kyiv")
                .branchName("Branch #12")
                .build();

        when(addressRepository.save(any(SavedAddress.class))).thenReturn(savedAddress);

        AddressDTO result = addressService.saveAddress(inputDto);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(addressId);
        assertThat(result.title()).isEqualTo("Home Nova Poshta");

        ArgumentCaptor<SavedAddress> captor = ArgumentCaptor.forClass(SavedAddress.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should return all saved addresses for current user")
    void getMyAddresses_ShouldReturnUserAddresses() {
        SavedAddress address = SavedAddress.builder()
                .id(UUID.randomUUID())
                .user(currentUser)
                .title("Base Address")
                .provider(DeliveryProvider.UKRPOSHTA)
                .deliveryType(DeliveryType.COURIER)
                .cityName("Lviv")
                .build();

        when(addressRepository.findAllByUserId(userId)).thenReturn(List.of(address));

        List<AddressDTO> result = addressService.getMyAddresses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).cityName()).isEqualTo("Lviv");
    }

    @Test
    @DisplayName("Should throw BusinessException when non-seller attempts to add pickup point")
    void addSellerPoint_WhenNotSeller_ShouldThrowException() {
        currentUser.setSellerProfile(null);

        SellerPointDTO dto = new SellerPointDTO(
                null, "Pickup 1", "ref", "Kyiv", "Kyiv", "Main", "1", null, "01001", "0501234567", "Ring 1"
        );

        assertThatThrownBy(() -> addressService.addSellerPoint(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User is not a seller");

        verify(sellerPointRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should add pickup point successfully when user is a seller")
    void addSellerPoint_Success_ShouldSaveAndReturnDTO() {
        SellerProfile sellerProfile = SellerProfile.builder()
                .id(UUID.randomUUID())
                .build();
        currentUser.setSellerProfile(sellerProfile);

        SellerPointDTO dto = new SellerPointDTO(
                null, "Tactical Warehouse", "ref", "Dnipro", "Dnipro", "Heroiv", "15", null, "49000", "0509876543", "Behind gate"
        );

        UUID pointId = UUID.randomUUID();
        SellerPoint savedPoint = SellerPoint.builder()
                .id(pointId)
                .sellerProfile(sellerProfile)
                .name("Tactical Warehouse")
                .cityName("Dnipro")
                .phone("0509876543")
                .build();

        when(sellerPointRepository.save(any(SellerPoint.class))).thenReturn(savedPoint);

        SellerPointDTO result = addressService.addSellerPoint(dto);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(pointId);
        assertThat(result.name()).isEqualTo("Tactical Warehouse");
        verify(sellerPointRepository).save(any(SellerPoint.class));
    }

    @Test
    @DisplayName("Should return empty list of seller points when user has no seller profile")
    void getMySellerPoints_WhenNotSeller_ShouldReturnEmptyList() {
        currentUser.setSellerProfile(null);

        List<SellerPointDTO> result = addressService.getMySellerPoints();

        assertThat(result).isEmpty();
        verify(sellerPointRepository, never()).findAllBySellerProfileId(any());
    }
}
