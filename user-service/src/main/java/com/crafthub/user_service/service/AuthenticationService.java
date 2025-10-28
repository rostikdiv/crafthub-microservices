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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // 1. Створюємо нового юзера (використовуємо Builder, який ми додали)
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // ❗️ Хешуємо пароль
                .role(Role.USER) // ❗️ За замовчуванням - USER
                .build();

        // 2. Зберігаємо в базу
        userRepository.save(user);

        // 3. Генеруємо JWT токен
        var jwtToken = jwtService.generateToken(user);

        // 4. Повертаємо токен у DTO
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        // 1. "Менеджер" автентифікації перевіряє email та пароль
        // Він використовує наш UserDetailsService та PasswordEncoder
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // 2. Якщо помилки немає (пароль вірний), знаходимо юзера
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(); // Ми впевнені, що він є, бо (1) пройшло

        // 3. Генеруємо JWT токен
        var jwtToken = jwtService.generateToken(user);

        // 4. Повертаємо токен
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}