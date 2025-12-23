package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.AuthenticationResponse;
import com.crafthub.user_service.dto.LoginRequest;
import com.crafthub.user_service.dto.RegisterRequest;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // Якщо роль не вказана, вважаємо, що це звичайний покупець
        var role = request.getRole() == null ? Role.BUYER : request.getRole();

        // Військові та продавці вимагають верифікації. Покупці - ні.
        boolean isVerified = role == Role.BUYER;

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber()) // 🆕 Зберігаємо телефон
                .role(role)
                .isVerified(isVerified) // 🆕 Логіка верифікації
                .build();

        repository.save(user);

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}