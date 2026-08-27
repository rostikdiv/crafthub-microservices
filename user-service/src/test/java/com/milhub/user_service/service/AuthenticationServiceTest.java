package com.milhub.user_service.service;

import com.milhub.user_service.dto.auth.AuthenticationResponse;
import com.milhub.user_service.dto.auth.LoginRequest;
import com.milhub.user_service.dto.auth.RegisterRequest;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

//    // === 1. Create mock dependencies ===
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private PasswordEncoder passwordEncoder;
//    @Mock
//    private JwtService jwtService;
//    @Mock
//    private AuthenticationManager authenticationManager;
//
//    // === 2. Inject Mocks ===
//    @InjectMocks
//    private AuthenticationService authenticationService;
//
//    // === 3. Test Methods ===
//
//    @Test
//    @DisplayName("Should Register User Successfully")
//    void shouldRegisterUserSuccessfully() {
//        // --- 1. ARRANGE ---
//
//        // Input data
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("Test")
//                .lastName("User")
//                .email("test@user.com")
//                .password("password123")
//                .build();
//
//        String fakeHashedPassword = "hashed_password_abc123";
//        String fakeJwtToken = "mock.jwt.token";
//
//        // Mock behaviors:
//        // "WHEN passwordEncoder.encode("password123") is called..."
//        when(passwordEncoder.encode("password123"))
//                .thenReturn(fakeHashedPassword); // "...return fake hash"
//
//        // "WHEN jwtService.generateToken() is called with ANY User..."
//        when(jwtService.generateToken(any(User.class)))
//                .thenReturn(fakeJwtToken); // "...return fake token"
//
//        // Create ArgumentCaptor to capture the User object
//        // that will be passed to userRepository.save()
//        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
//
//        // --- 2. ACT ---
//
//        // Call registration method
//        AuthenticationResponse response = authenticationService.register(request);
//
//        // --- 3. ASSERT ---
//
//        // A) Verify response (token returned)
//        assertThat(response).isNotNull();
//        assertThat(response.getToken()).isEqualTo(fakeJwtToken);
//
//        // B) Verify save method was called once
//        verify(userRepository).save(userArgumentCaptor.capture());
//
//        // C) Retrieve captured User and verify fields
//        User savedUser = userArgumentCaptor.getValue();
//        assertThat(savedUser.getEmail()).isEqualTo("test@user.com");
//        assertThat(savedUser.getFirstName()).isEqualTo("Test");
//        assertThat(savedUser.getRole()).isEqualTo(Role.USER); // Role check
//        assertThat(savedUser.getPassword()).isEqualTo(fakeHashedPassword); // Critical password check!
//    }
//
//    @Test
//    @DisplayName("Should Login User Successfully")
//    void shouldLoginUserSuccessfully() {
//        // --- 1. ARRANGE ---
//
//        // Input data
//        LoginRequest request = LoginRequest.builder()
//                .email("test@user.com")
//                .password("password123")
//                .build();
//
//        // Create mock User to be returned by repository
//        User mockUser = User.builder()
//                .email("test@user.com")
//                .password("hashed_password") // Irrelevant value for mock
//                .role(Role.USER)
//                .build();
//
//        String fakeJwtToken = "mock.jwt.token";
//
//        // Mock behaviors:
//        // "WHEN authenticationManager.authenticate() is called...
//        // ...it should complete successfully without returning anything"
//        // (For void methods, Mockito does nothing by default)
//
//        // "WHEN userRepository.findByEmail("test@user.com") is called..."
//        when(userRepository.findByEmail("test@user.com"))
//                .thenReturn(Optional.of(mockUser)); // "...return fake User"
//
//        // "WHEN jwtService.generateToken(mockUser)..."
//        when(jwtService.generateToken(mockUser))
//                .thenReturn(fakeJwtToken); // "...return fake token"
//
//        // --- 2. ACT ---
//        AuthenticationResponse response = authenticationService.login(request);
//
//        // --- 3. ASSERT ---
//
//        // A) Verify response
//        assertThat(response).isNotNull();
//        assertThat(response.getToken()).isEqualTo(fakeJwtToken);
//
//        // B) Verify AuthenticationManager was called once with correct credentials
//        verify(authenticationManager).authenticate(
//                new UsernamePasswordAuthenticationToken(
//                        "test@user.com",
//                        "password123"
//                )
//        );
//
//        // C) Verify repository was queried once
//        verify(userRepository).findByEmail("test@user.com");
//    }
}