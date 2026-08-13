package com.milhub.user_service.service;

import com.milhub.user_service.dto.auth.AuthenticationResponse;
import com.milhub.user_service.dto.auth.LoginRequest;
import com.milhub.user_service.dto.auth.RegisterRequest;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for handling user registration and authentication logic.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user. Validates email uniqueness before persistence.
     *
     * @param request User registration details.
     * @return AuthenticationResponse containing the JWT token.
     */
    public AuthenticationResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("User with this email already exists");
        }

        var requestedRole = request.getRole();
        if (requestedRole == Role.ADMIN) {
            throw new BusinessException("Cannot self-register as an administrator.");
        }

        // All new self-registered accounts start as BUYER with isVerified = false.
        // Role upgrades to SELLER or MILITARY_UNIT occur via Admin verification (AdminService.verifyUser).
        var role = Role.BUYER;
        boolean isVerified = false;

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .isVerified(isVerified)
                .build();

        repository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    /**
     * Authenticates a user based on email and password.
     *
     * @param request Login credentials.
     * @return AuthenticationResponse containing the JWT token.
     */
    public AuthenticationResponse authenticate(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
        } catch (BadCredentialsException e) {
            // Rethrow to be caught by GlobalExceptionHandler (resulting in 401
            // Unauthorized)
            throw e;
        }

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}