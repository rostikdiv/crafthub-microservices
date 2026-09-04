package com.milhub.user_service.service;

import com.milhub.user_service.dto.profile.MilitaryProfileRequestDTO;
import com.milhub.user_service.dto.profile.SellerProfileRequestDTO;
import com.milhub.user_service.dto.profile.VerificationDocRequestDTO;
import com.milhub.user_service.entity.*;
import com.milhub.user_service.entity.enums.DocumentType;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.*;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SellerProfileRepository sellerProfileRepository;

    @Mock
    private MilitaryProfileRepository militaryProfileRepository;

    @Mock
    private VerificationDocRepository verificationDocRepository;

    @InjectMocks
    private ProfileService profileService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("user@milhub.ua")
                .firstName("Stepan")
                .lastName("Bandera")
                .role(Role.BUYER)
                .isVerified(false)
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(userId.toString());
        SecurityContextHolder.setContext(securityContext);

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should throw BusinessException when seller profile already exists")
    void createSellerProfile_WhenAlreadyExists_ShouldThrowException() {
        when(sellerProfileRepository.findByUserId(userId)).thenReturn(Optional.of(new SellerProfile()));

        SellerProfileRequestDTO dto = new SellerProfileRequestDTO();
        dto.setCompanyName("MilHub Shop");

        assertThatThrownBy(() -> profileService.createSellerProfile(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Seller profile already created");

        verify(sellerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create seller profile and update user role to SELLER")
    void createSellerProfile_Success_ShouldSaveAndUpgradeRole() {
        when(sellerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        SellerProfileRequestDTO dto = new SellerProfileRequestDTO();
        dto.setCompanyName("MilHub Shop");
        dto.setDescription("Tactical gear store");
        dto.setTaxId("12345678");
        dto.setLogoUrl("http://logo.png");
        dto.setAutoConfirmOrders(true);

        profileService.createSellerProfile(dto);

        ArgumentCaptor<SellerProfile> profileCaptor = ArgumentCaptor.forClass(SellerProfile.class);
        verify(sellerProfileRepository).save(profileCaptor.capture());
        SellerProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getCompanyName()).isEqualTo("MilHub Shop");
        assertThat(savedProfile.getAutoConfirmOrders()).isTrue();

        assertThat(testUser.getRole()).isEqualTo(Role.SELLER);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw BusinessException when military profile already exists")
    void createMilitaryProfile_WhenAlreadyExists_ShouldThrowException() {
        when(militaryProfileRepository.findByUserId(userId)).thenReturn(Optional.of(new MilitaryProfile()));

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A1234");

        assertThatThrownBy(() -> profileService.createMilitaryProfile(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Military profile already created");

        verify(militaryProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create military profile and update role to MILITARY_UNIT")
    void createMilitaryProfile_Success_ShouldSaveAndUpgradeRole() {
        when(militaryProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A7015");
        dto.setEdrpou("12345678");
        dto.setCommanderName("General");
        dto.setOfficialAddress("Kyiv, Main St");

        profileService.createMilitaryProfile(dto);

        ArgumentCaptor<MilitaryProfile> captor = ArgumentCaptor.forClass(MilitaryProfile.class);
        verify(militaryProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUnitNumber()).isEqualTo("A7015");

        assertThat(testUser.getRole()).isEqualTo(Role.MILITARY_UNIT);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent seller profile")
    void updateSellerProfile_WhenNotCreated_ShouldThrowException() {
        testUser.setSellerProfile(null);

        SellerProfileRequestDTO dto = new SellerProfileRequestDTO();
        dto.setCompanyName("Updated Name");

        assertThatThrownBy(() -> profileService.updateSellerProfile(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Seller profile not created yet");
    }

    @Test
    @DisplayName("Should update seller profile fields successfully")
    void updateSellerProfile_Success_ShouldUpdateFields() {
        SellerProfile profile = SellerProfile.builder()
                .companyName("Old Name")
                .description("Old Desc")
                .build();
        testUser.setSellerProfile(profile);

        SellerProfileRequestDTO dto = new SellerProfileRequestDTO();
        dto.setCompanyName("New Name");
        dto.setDescription("New Desc");
        dto.setLogoUrl("http://new-logo.png");
        dto.setAutoConfirmOrders(false);

        profileService.updateSellerProfile(dto);

        assertThat(profile.getCompanyName()).isEqualTo("New Name");
        assertThat(profile.getDescription()).isEqualTo("New Desc");
        assertThat(profile.getLogoUrl()).isEqualTo("http://new-logo.png");
        assertThat(profile.getAutoConfirmOrders()).isFalse();
        verify(sellerProfileRepository).save(profile);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent military profile")
    void updateMilitaryProfile_WhenNotCreated_ShouldThrowException() {
        testUser.setMilitaryProfile(null);

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A9999");

        assertThatThrownBy(() -> profileService.updateMilitaryProfile(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Military profile not created yet");
    }

    @Test
    @DisplayName("Should update military profile, reset isVerified to false and downgrade role to BUYER")
    void updateMilitaryProfile_WhenChanged_ShouldResetVerification() {
        MilitaryProfile profile = MilitaryProfile.builder()
                .unitNumber("A1111")
                .edrpou("11111111")
                .build();
        testUser.setMilitaryProfile(profile);
        testUser.setIsVerified(true);
        testUser.setRole(Role.MILITARY_UNIT);

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A2222"); // Changed
        dto.setEdrpou("11111111");

        profileService.updateMilitaryProfile(dto);

        assertThat(profile.getUnitNumber()).isEqualTo("A2222");
        assertThat(testUser.getIsVerified()).isFalse();
        assertThat(testUser.getRole()).isEqualTo(Role.BUYER);
        verify(militaryProfileRepository).save(profile);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should add verification document with PENDING status")
    void addVerificationDocument_Success_ShouldSaveWithPending() {
        VerificationDocRequestDTO dto = new VerificationDocRequestDTO(DocumentType.PASSPORT, "http://doc.url");

        profileService.addVerificationDocument(dto);

        ArgumentCaptor<VerificationDoc> captor = ArgumentCaptor.forClass(VerificationDoc.class);
        verify(verificationDocRepository).save(captor.capture());
        VerificationDoc savedDoc = captor.getValue();
        assertThat(savedDoc.getDocumentType()).isEqualTo(DocumentType.PASSPORT);
        assertThat(savedDoc.getDocUrl()).isEqualTo("http://doc.url");
        assertThat(savedDoc.getStatus()).isEqualTo(VerificationStatus.PENDING);
    }

    @Test
    @DisplayName("updateMilitaryProfile: when no fields changed, does not reset verification")
    void updateMilitaryProfile_WhenNoChanges_ShouldNotResetVerification() {
        MilitaryProfile profile = MilitaryProfile.builder()
                .unitNumber("A1111")
                .edrpou("11111111")
                .commanderName("Commander")
                .officialAddress("Address")
                .build();
        testUser.setMilitaryProfile(profile);
        testUser.setIsVerified(true);
        testUser.setRole(Role.MILITARY_UNIT);

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A1111");
        dto.setEdrpou("11111111");
        dto.setCommanderName("Commander");
        dto.setOfficialAddress("Address");

        profileService.updateMilitaryProfile(dto);

        assertThat(testUser.getIsVerified()).isTrue();
        assertThat(testUser.getRole()).isEqualTo(Role.MILITARY_UNIT);
        verify(militaryProfileRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateMilitaryProfile: detects individual changes in commanderName, edrpou, officialAddress")
    void updateMilitaryProfile_WhenCommanderOrAddressChanges_ShouldReset() {
        MilitaryProfile profile = MilitaryProfile.builder()
                .unitNumber("A1111")
                .edrpou("11111111")
                .commanderName("Old Commander")
                .officialAddress("Old Address")
                .build();
        testUser.setMilitaryProfile(profile);
        testUser.setIsVerified(true);

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A1111");
        dto.setEdrpou("22222222"); // changed
        dto.setCommanderName("New Commander"); // changed
        dto.setOfficialAddress("New Address"); // changed

        profileService.updateMilitaryProfile(dto);

        assertThat(profile.getEdrpou()).isEqualTo("22222222");
        assertThat(profile.getCommanderName()).isEqualTo("New Commander");
        assertThat(profile.getOfficialAddress()).isEqualTo("New Address");
        assertThat(testUser.getIsVerified()).isFalse();
    }

    @Test
    @DisplayName("createSellerProfile: when user is ADMIN, does not overwrite role to SELLER")
    void createSellerProfile_WhenAdmin_ShouldKeepAdminRole() {
        testUser.setRole(Role.ADMIN);
        when(sellerProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        SellerProfileRequestDTO dto = new SellerProfileRequestDTO();
        dto.setCompanyName("Admin Shop");

        profileService.createSellerProfile(dto);

        assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(testUser);
    }

    @Test
    @DisplayName("createMilitaryProfile: when user is ADMIN, does not overwrite role to MILITARY_UNIT")
    void createMilitaryProfile_WhenAdmin_ShouldKeepAdminRole() {
        testUser.setRole(Role.ADMIN);
        when(militaryProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        MilitaryProfileRequestDTO dto = new MilitaryProfileRequestDTO();
        dto.setUnitNumber("A9999");

        profileService.createMilitaryProfile(dto);

        assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).save(testUser);
    }

    @Test
    @DisplayName("updateSellerProfile: when autoConfirmOrders is null, does not modify profile setting")
    void updateSellerProfile_WhenAutoConfirmOrdersNull_ShouldKeepExisting() {
        SellerProfile profile = SellerProfile.builder()
                .companyName("Old Name")
                .autoConfirmOrders(true)
                .build();
        testUser.setSellerProfile(profile);

        SellerProfileRequestDTO dto = new SellerProfileRequestDTO();
        dto.setCompanyName("New Name");
        dto.setAutoConfirmOrders(null);

        profileService.updateSellerProfile(dto);

        assertThat(profile.getAutoConfirmOrders()).isTrue();
    }
}
