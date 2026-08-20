package com.milhub.user_service.config;

import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${application.security.admin.email:admin@milhub.ua}")
    private String adminEmail;

    @Value("${application.security.admin.password:Password123!}")
    private String adminPassword;

    @Value("${application.security.admin.first-name:System}")
    private String adminFirstName;

    @Value("${application.security.admin.last-name:Admin}")
    private String adminLastName;

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminOnStartup() {
        try {
            userRepository.findByEmail(adminEmail).ifPresentOrElse(
                admin -> {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.ADMIN);
                    admin.setIsVerified(true);
                    userRepository.save(admin);
                    log.info("✅ Verified/Updated Admin user credentials: {}", adminEmail);
                },
                () -> {
                    User admin = User.builder()
                            .firstName(adminFirstName)
                            .lastName(adminLastName)
                            .email(adminEmail)
                            .password(passwordEncoder.encode(adminPassword))
                            .phoneNumber("+380000000000")
                            .role(Role.ADMIN)
                            .isVerified(true)
                            .build();
                    userRepository.save(admin);
                    log.info("✅ Provisioned initial Admin user: {}", adminEmail);
                }
            );
        } catch (Exception e) {
            log.error("Failed to auto-seed Admin user: {}", e.getMessage());
        }
    }
}
