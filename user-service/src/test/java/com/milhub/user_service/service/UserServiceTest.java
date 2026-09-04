package com.milhub.user_service.service;

import com.milhub.user_service.dto.auth.ChangePasswordRequestDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.dto.user.UserUpdateDTO;
import com.milhub.user_service.entity.MilitaryProfile;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("soldier@milhub.ua")
                .firstName("Oleksandr")
                .lastName("Sirskyi")
                .password("hashed_current_password")
                .role(Role.MILITARY_UNIT)
                .isVerified(true)
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
    @DisplayName("Should return user when found by ID")
    void getUserById_WhenFound_ShouldReturnUser() {
        User result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("soldier@milhub.ua");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found by ID")
    void getUserById_WhenNotFound_ShouldThrowException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: " + unknownId);
    }

    @Test
    @DisplayName("Should return UserResponseDTO with seller and military profile mappings")
    void getUserByIdWithProfiles_ShouldMapProfiles() {
        SellerProfile sellerProfile = SellerProfile.builder()
                .id(UUID.randomUUID())
                .companyName("UA Gear")
                .taxId("998877")
                .rating(4.8f)
                .reviewCount(10)
                .build();
        MilitaryProfile militaryProfile = MilitaryProfile.builder()
                .id(UUID.randomUUID())
                .unitNumber("A1234")
                .edrpou("12345678")
                .build();

        testUser.setSellerProfile(sellerProfile);
        testUser.setMilitaryProfile(militaryProfile);

        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.of(testUser));

        UserResponseDTO response = userService.getUserByIdWithProfiles(userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getSellerProfile()).isNotNull();
        assertThat(response.getSellerProfile().getCompanyName()).isEqualTo("UA Gear");
        assertThat(response.getMilitaryProfile()).isNotNull();
        assertThat(response.getMilitaryProfile().getUnitNumber()).isEqualTo("A1234");
    }

    @Test
    @DisplayName("Should update current user first and last name")
    void updateCurrentUser_ShouldUpdateNames() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setFirstName("UpdatedFirst");
        dto.setLastName("UpdatedLast");

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updated = userService.updateCurrentUser(dto);

        assertThat(testUser.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(testUser.getLastName()).isEqualTo("UpdatedLast");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should throw BusinessException when current password does not match")
    void changePassword_WhenCurrentPasswordIncorrect_ShouldThrowException() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
                "wrong_current",
                "NewPassword1!",
                "NewPassword1!"
        );

        when(passwordEncoder.matches("wrong_current", "hashed_current_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Incorrect current password");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when new password and confirmation do not match")
    void changePassword_WhenConfirmationMismatch_ShouldThrowException() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
                "correct_current",
                "NewPassword1!",
                "DifferentPassword2!"
        );

        when(passwordEncoder.matches("correct_current", "hashed_current_password")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Passwords do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update password successfully when inputs are valid")
    void changePassword_Success_ShouldEncodeAndSave() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO(
                "correct_current",
                "NewPassword1!",
                "NewPassword1!"
        );

        when(passwordEncoder.matches("correct_current", "hashed_current_password")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1!")).thenReturn("newly_hashed_password");

        userService.changePassword(dto);

        assertThat(testUser.getPassword()).isEqualTo("newly_hashed_password");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Should promote user to ADMIN and verify them")
    void promoteUserToAdmin_ShouldSetAdminRoleAndVerify() {
        testUser.setRole(Role.BUYER);
        testUser.setIsVerified(false);

        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User promoted = userService.promoteUserToAdmin(userId);

        assertThat(promoted.getRole()).isEqualTo(Role.ADMIN);
        assertThat(promoted.getIsVerified()).isTrue();
        verify(userRepository).save(testUser);
    }
}
