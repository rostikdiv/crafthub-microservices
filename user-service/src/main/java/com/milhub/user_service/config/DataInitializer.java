package com.milhub.user_service.config;

import com.milhub.user_service.entity.User;
import com.milhub.user_service.entity.enums.Role;
import com.milhub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @EventListener(ApplicationReadyEvent.class)
    public void seedAdminOnStartup() {
        try {
            if (userRepository.findByEmail("admin@milhub.ua").isEmpty()) {
                User admin = User.builder()
                        .firstName("System")
                        .lastName("Admin")
                        .email("admin@milhub.ua")
                        .password(passwordEncoder.encode("Password123!"))
                        .phoneNumber("+380000000000")
                        .role(Role.ADMIN)
                        .isVerified(true)
                        .build();
                userRepository.save(admin);
                log.info("✅ Auto-seeded Admin user: admin@milhub.ua");
            }
        } catch (Exception e) {
            log.error("Failed to auto-seed Admin user: {}", e.getMessage());
        }
    }
}
