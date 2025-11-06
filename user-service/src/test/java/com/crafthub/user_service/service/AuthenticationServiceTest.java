package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.AuthenticationResponse;
import com.crafthub.user_service.dto.LoginRequest;
import com.crafthub.user_service.dto.RegisterRequest;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.repository.UserRepository;
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

    // === 1. Створення "фальшивих" залежностей (Mocks) ===
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    // === 2. Впровадження Моків (InjectMocks) ===
    @InjectMocks
    private AuthenticationService authenticationService;

    // === 3. Тестові Методи ===

    @Test
    @DisplayName("Should Register User Successfully")
    void shouldRegisterUserSuccessfully() {
        // --- 1. ARRANGE (Налаштування) ---

        // Вхідні дані
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@user.com")
                .password("password123")
                .build();

        String fakeHashedPassword = "hashed_password_abc123";
        String fakeJwtToken = "mock.jwt.token";

        // Навчаємо Моки:
        // "КОЛИ passwordEncoder.encode("password123") буде викликаний..."
        when(passwordEncoder.encode("password123"))
                .thenReturn(fakeHashedPassword); // "...повернути фальшивий хеш"

        // "КОЛИ jwtService.generateToken() буде викликаний з БУДЬ-ЯКИМ User..."
        when(jwtService.generateToken(any(User.class)))
                .thenReturn(fakeJwtToken); // "...повернути фальшивий токен"

        // Створюємо "пастку" (ArgumentCaptor), щоб зловити об'єкт User,
        // який буде переданий у userRepository.save()
        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        // --- 2. ACT (Дія) ---

        // Викликаємо метод реєстрації
        AuthenticationResponse response = authenticationService.register(request);

        // --- 3. ASSERT (Перевірка) ---

        // A) Перевіряємо відповідь (чи повернули ми токен)
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(fakeJwtToken);

        // B) Перевіряємо, що метод save був викликаний 1 раз
        verify(userRepository).save(userArgumentCaptor.capture());

        // C) Дістаємо "зловленого" User і перевіряємо його поля
        User savedUser = userArgumentCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("test@user.com");
        assertThat(savedUser.getFirstName()).isEqualTo("Test");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER); // Перевірка ролі
        assertThat(savedUser.getPassword()).isEqualTo(fakeHashedPassword); // Критична перевірка!
    }

    @Test
    @DisplayName("Should Login User Successfully")
    void shouldLoginUserSuccessfully() {
        // --- 1. ARRANGE (Налаштування) ---

        // Вхідні дані
        LoginRequest request = LoginRequest.builder()
                .email("test@user.com")
                .password("password123")
                .build();

        // Створюємо фальшивого User, якого "знайде" репозиторій
        User mockUser = User.builder()
                .email("test@user.com")
                .password("hashed_password") // Неважливо, що тут
                .role(Role.USER)
                .build();

        String fakeJwtToken = "mock.jwt.token";

        // Навчаємо Моки:
        // "КОЛИ authenticationManager.authenticate() буде викликаний...
        // ...він має просто успішно відпрацювати (нічого не повертати)"
        // (Для void методів Mockito нічого не робить за замовчуванням - це ідеально)

        // "КОЛИ userRepository.findByEmail("test@user.com") буде викликаний..."
        when(userRepository.findByEmail("test@user.com"))
                .thenReturn(Optional.of(mockUser)); // "...повернути нашого fake User"

        // "КОЛИ jwtService.generateToken(mockUser)..."
        when(jwtService.generateToken(mockUser))
                .thenReturn(fakeJwtToken); // "...повернути fake token"

        // --- 2. ACT (Дія) ---
        AuthenticationResponse response = authenticationService.login(request);

        // --- 3. ASSERT (Перевірка) ---

        // A) Перевіряємо відповідь
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(fakeJwtToken);

        // B) Перевіряємо, що AuthenticationManager був викликаний 1 раз
        // з правильними даними (email та паролем)
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken(
                        "test@user.com",
                        "password123"
                )
        );

        // C) Перевіряємо, що ми 1 раз шукали юзера в базі
        verify(userRepository).findByEmail("test@user.com");
    }
}