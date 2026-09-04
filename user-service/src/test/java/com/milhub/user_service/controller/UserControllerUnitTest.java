package com.milhub.user_service.controller;

import com.milhub.user_service.dto.auth.ChangePasswordRequestDTO;
import com.milhub.user_service.dto.seller.SellerInfoDTO;
import com.milhub.user_service.dto.user.UserResponseDTO;
import com.milhub.user_service.dto.user.UserUpdateDTO;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import com.milhub.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private final UUID userId = UUID.randomUUID();

    @Test
    void getAllUsers_ShouldMapToResponseDTOs() {
        User user = User.builder().id(userId).build();
        UserResponseDTO dto = UserResponseDTO.builder().id(userId).build();

        when(userRepository.findAllWithProfiles()).thenReturn(List.of(user));
        when(userService.mapToResponseDTO(user)).thenReturn(dto);

        ResponseEntity<List<UserResponseDTO>> response = controller.getAllUsers();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(dto);
    }

    @Test
    void getUserById_ShouldCallService() {
        UserResponseDTO dto = UserResponseDTO.builder().id(userId).build();
        when(userService.getUserByIdWithProfiles(userId)).thenReturn(dto);

        ResponseEntity<UserResponseDTO> response = controller.getUserById(userId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dto);
        verify(userService).getUserByIdWithProfiles(userId);
    }

    @Test
    void deleteUser_WhenExists_ShouldDelete() {
        when(userRepository.existsById(userId)).thenReturn(true);

        ResponseEntity<Void> response = controller.deleteUser(userId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_WhenNotExists_ShouldThrowException() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> controller.deleteUser(userId));
    }

    @Test
    void getSellerInfo_WhenValidSeller_ShouldReturnDTO() {
        SellerProfile profile = SellerProfile.builder()
                .companyName("MilStore")
                .logoUrl("http://logo.png")
                .rating(4.8f)
                .reviewCount(12)
                .totalSales(150)
                .autoConfirmOrders(true)
                .build();

        User user = User.builder()
                .id(userId)
                .isVerified(true)
                .sellerProfile(profile)
                .build();

        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.of(user));

        ResponseEntity<SellerInfoDTO> response = controller.getSellerInfo(userId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().companyName()).isEqualTo("MilStore");
        assertThat(response.getBody().isVerified()).isTrue();
    }

    @Test
    void getSellerInfo_WhenUserNotFound_ShouldThrowException() {
        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> controller.getSellerInfo(userId));
    }

    @Test
    void getSellerInfo_WhenUserNotSeller_ShouldThrowException() {
        User user = User.builder().id(userId).sellerProfile(null).build();
        when(userRepository.findByIdWithProfiles(userId)).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> controller.getSellerInfo(userId));
    }

    @Test
    void updateCurrentUser_ShouldCallService() {
        UserUpdateDTO dto = new UserUpdateDTO("Petro", "Petrov");
        User user = User.builder().id(userId).firstName("Petro").build();

        when(userService.updateCurrentUser(dto)).thenReturn(user);

        ResponseEntity<User> response = controller.updateCurrentUser(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(user);
        verify(userService).updateCurrentUser(dto);
    }

    @Test
    void changePassword_ShouldCallService() {
        ChangePasswordRequestDTO dto = new ChangePasswordRequestDTO("oldPass", "newPass", "newPass");

        ResponseEntity<Void> response = controller.changePassword(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(userService).changePassword(dto);
    }
}
