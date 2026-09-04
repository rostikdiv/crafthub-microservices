package com.milhub.user_service.controller;

import com.milhub.user_service.dto.auth.AuthenticationResponse;
import com.milhub.user_service.dto.auth.LoginRequest;
import com.milhub.user_service.dto.auth.RegisterRequest;
import com.milhub.user_service.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authService;

    @InjectMocks
    private AuthenticationController controller;

    @Test
    void register_ShouldCallService() {
        RegisterRequest request = new RegisterRequest("Ivan", "Ivanov", "ivan@test.com", "pass123", "+380501112233", com.milhub.user_service.entity.enums.Role.BUYER);
        AuthenticationResponse authResponse = AuthenticationResponse.builder().token("jwt-123").build();

        when(authService.register(request)).thenReturn(authResponse);

        ResponseEntity<AuthenticationResponse> response = controller.register(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(authResponse);
        verify(authService).register(request);
    }

    @Test
    void authenticate_ShouldCallService() {
        LoginRequest request = new LoginRequest("ivan@test.com", "pass123");
        AuthenticationResponse authResponse = AuthenticationResponse.builder().token("jwt-456").build();

        when(authService.authenticate(request)).thenReturn(authResponse);

        ResponseEntity<AuthenticationResponse> response = controller.authenticate(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(authResponse);
        verify(authService).authenticate(request);
    }
}
