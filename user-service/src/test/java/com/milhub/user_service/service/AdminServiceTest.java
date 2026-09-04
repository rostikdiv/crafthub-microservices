package com.milhub.user_service.service;

import com.milhub.user_service.dto.admin.VerificationRequestResponseDTO;
import com.milhub.user_service.dto.user.UserVerificationEvent;
import com.milhub.user_service.entity.MilitaryProfile;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.VerificationDoc;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.entity.enums.VerificationStatus;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import com.milhub.user_service.repository.VerificationDocRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final String USER_VERIFICATION_TOPIC = "user-verification-topic";

    @Mock
    private UserRepository userRepository;

    @Mock
    private VerificationDocRepository docRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AdminService adminService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("soldier@milhub.ua")
                .firstName("Taras")
                .lastName("Shevchenko")
                .role(Role.BUYER)
                .isVerified(false)
                .createdAt(Timestamp.from(Instant.now()))
                .documents(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should return pending verification requests with correct metadata")
    void getPendingVerifications_ShouldReturnMappedDTOs() {
        MilitaryProfile militaryProfile = MilitaryProfile.builder()
                .unitNumber("A0123")
                .build();
        testUser.setMilitaryProfile(militaryProfile);

        VerificationDoc doc = VerificationDoc.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .status(VerificationStatus.PENDING)
                .build();
        testUser.getDocuments().add(doc);

        when(userRepository.findUsersWithPendingDocuments()).thenReturn(List.of(testUser));

        List<VerificationRequestResponseDTO> result = adminService.getPendingVerifications();

        assertThat(result).hasSize(1);
        VerificationRequestResponseDTO dto = result.get(0);
        assertThat(dto.userId()).isEqualTo(userId);
        assertThat(dto.email()).isEqualTo("soldier@milhub.ua");
        assertThat(dto.specificName()).isEqualTo("A0123");
        assertThat(dto.pendingDocsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when verifying non-existent user")
    void verifyUser_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.verifyUser(userId, true, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: " + userId);

        verify(kafkaTemplate, never()).send(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should upgrade role to MILITARY_UNIT and publish Kafka event when military user is verified")
    void verifyUser_WhenMilitaryProfile_ShouldUpgradeToMilitaryUnitAndSendKafka() {
        MilitaryProfile militaryProfile = MilitaryProfile.builder()
                .unitNumber("A4444")
                .build();
        testUser.setMilitaryProfile(militaryProfile);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.verifyUser(userId, true, null);

        assertThat(testUser.getIsVerified()).isTrue();
        assertThat(testUser.getRole()).isEqualTo(Role.MILITARY_UNIT);
        verify(userRepository).save(testUser);

        ArgumentCaptor<UserVerificationEvent> eventCaptor = ArgumentCaptor.forClass(UserVerificationEvent.class);
        verify(kafkaTemplate).send(eq(USER_VERIFICATION_TOPIC), eventCaptor.capture());

        UserVerificationEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("soldier@milhub.ua");
        assertThat(event.isVerified()).isTrue();
        assertThat(event.reason()).isNull();
    }

    @Test
    @DisplayName("Should upgrade role to SELLER and publish Kafka event when seller user is verified")
    void verifyUser_WhenSellerProfile_ShouldUpgradeToSellerAndSendKafka() {
        SellerProfile sellerProfile = SellerProfile.builder()
                .companyName("Tactical Gear UA")
                .build();
        testUser.setSellerProfile(sellerProfile);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.verifyUser(userId, true, null);

        assertThat(testUser.getIsVerified()).isTrue();
        assertThat(testUser.getRole()).isEqualTo(Role.SELLER);
        verify(userRepository).save(testUser);

        ArgumentCaptor<UserVerificationEvent> eventCaptor = ArgumentCaptor.forClass(UserVerificationEvent.class);
        verify(kafkaTemplate).send(eq(USER_VERIFICATION_TOPIC), eventCaptor.capture());

        UserVerificationEvent event = eventCaptor.getValue();
        assertThat(event.isVerified()).isTrue();
    }

    @Test
    @DisplayName("Should reject verification with reason and publish Kafka event without upgrading role")
    void verifyUser_WhenRejected_ShouldKeepRoleAndPublishReason() {
        testUser.setRole(Role.BUYER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String rejectionReason = "Document expired or unreadable";
        adminService.verifyUser(userId, false, rejectionReason);

        assertThat(testUser.getIsVerified()).isFalse();
        assertThat(testUser.getRole()).isEqualTo(Role.BUYER);
        verify(userRepository).save(testUser);

        ArgumentCaptor<UserVerificationEvent> eventCaptor = ArgumentCaptor.forClass(UserVerificationEvent.class);
        verify(kafkaTemplate).send(eq(USER_VERIFICATION_TOPIC), eventCaptor.capture());

        UserVerificationEvent event = eventCaptor.getValue();
        assertThat(event.isVerified()).isFalse();
        assertThat(event.reason()).isEqualTo(rejectionReason);
    }

    @Test
    @DisplayName("Resilience: when KafkaTemplate throws exception, transaction does not fail and user is saved")
    void verifyUser_WhenKafkaFails_ShouldStillSaveUserGracefully() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(eq(USER_VERIFICATION_TOPIC), any()))
                .thenThrow(new RuntimeException("Kafka broker unreachable"));

        // Should complete without throwing exception
        adminService.verifyUser(userId, true, null);

        assertThat(testUser.getIsVerified()).isTrue();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("getPendingVerifications: when user has seller profile, sets companyName")
    void getPendingVerifications_WhenSellerProfile_ShouldSetCompanyName() {
        SellerProfile sellerProfile = SellerProfile.builder().companyName("Tactical Store").build();
        testUser.setSellerProfile(sellerProfile);
        when(userRepository.findUsersWithPendingDocuments()).thenReturn(List.of(testUser));

        List<VerificationRequestResponseDTO> result = adminService.getPendingVerifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).specificName()).isEqualTo("Tactical Store");
    }

    @Test
    @DisplayName("getPendingVerifications: when user has no special profile, sets N/A")
    void getPendingVerifications_WhenNoProfile_ShouldSetNA() {
        testUser.setSellerProfile(null);
        testUser.setMilitaryProfile(null);
        when(userRepository.findUsersWithPendingDocuments()).thenReturn(List.of(testUser));

        List<VerificationRequestResponseDTO> result = adminService.getPendingVerifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).specificName()).isEqualTo("N/A");
    }

    @Test
    @DisplayName("verifyUser: when user has neither military nor seller profile, verifies without role change")
    void verifyUser_WhenNoSpecialProfile_ShouldVerifyWithoutRoleUpgrade() {
        testUser.setRole(Role.BUYER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        adminService.verifyUser(userId, true, null);

        assertThat(testUser.getIsVerified()).isTrue();
        assertThat(testUser.getRole()).isEqualTo(Role.BUYER);
    }
}
