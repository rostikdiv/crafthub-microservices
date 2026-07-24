package com.crafthub.user_service.config;

import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.enums.Role;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes a master admin account on application startup if it doesn't already exist.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@crafthub.com";
        
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Master admin account not found. Creating...");
            
            User admin = User.builder()
                    .firstName("Master")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("password123"))
                    .phoneNumber("+1234567890")
                    .role(Role.ADMIN)
                    .isVerified(true) // Admins don't need offline verification
                    .build();
            
            userRepository.save(admin);
            log.info("Master admin account created successfully. Email: {}", adminEmail);
        } else {
            log.info("Master admin account already exists.");
        }
    }
}
