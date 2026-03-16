package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.auth.AuthenticationResponse;
import com.crafthub.user_service.dto.auth.LoginRequest;
import com.crafthub.user_service.dto.auth.RegisterRequest;
import com.crafthub.user_service.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user authentication operations.
 * Handles registration and login (authentication) requests.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService service;

    /**
     * Registers a new user in the system.
     *
     * @param request The registration details.
     * @return A ResponseEntity containing the authentication token.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(service.register(request));
    }

    /**
     * Authenticates an existing user and returns a JWT token.
     *
     * @param request The login credentials (email and password).
     * @return A ResponseEntity containing the authentication token.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }
}