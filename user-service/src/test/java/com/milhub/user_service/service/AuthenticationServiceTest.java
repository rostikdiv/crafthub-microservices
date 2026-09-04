package com.milhub.user_service.service;

import com.milhub.user_service.dto.auth.AuthenticationResponse;
import com.milhub.user_service.dto.auth.LoginRequest;
import com.milhub.user_service.dto.auth.RegisterRequest;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Should register user successfully with default BUYER role")
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Taras")
                .lastName("Shevchenko")
                .email("taras@milhub.ua")
                .password("password123")
                .build();

        when(userRepository.findByEmail("taras@milhub.ua")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed_secret");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt.token.val");

        AuthenticationResponse response = authenticationService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.val");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("taras@milhub.ua");
        assertThat(savedUser.getRole()).isEqualTo(Role.BUYER);
        assertThat(savedUser.getIsVerified()).isFalse();
        assertThat(savedUser.getPassword()).isEqualTo("hashed_secret");
    }

    @Test
    @DisplayName("Should throw BusinessException when registering with duplicate email")
    void register_WhenDuplicateEmail_ShouldThrowBusinessException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@milhub.ua")
                .password("password123")
                .build();

        when(userRepository.findByEmail("existing@milhub.ua")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessException when attempting to self-register as ADMIN")
    void register_WhenAdminRoleRequested_ShouldThrowBusinessException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@milhub.ua")
                .password("password123")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmail("admin@milhub.ua")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot self-register as an administrator");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user successfully with valid credentials")
    void shouldLoginUserSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("taras@milhub.ua")
                .password("password123")
                .build();

        User mockUser = User.builder()
                .email("taras@milhub.ua")
                .role(Role.BUYER)
                .build();

        when(userRepository.findByEmail("taras@milhub.ua")).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(mockUser)).thenReturn("jwt.token.val");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.val");

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("taras@milhub.ua", "password123")
        );
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when login authentication fails")
    void login_WhenInvalidCredentials_ShouldThrowBadCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("taras@milhub.ua")
                .password("wrong-pass")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }
}