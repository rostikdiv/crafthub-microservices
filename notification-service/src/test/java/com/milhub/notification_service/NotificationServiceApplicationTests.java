package com.milhub.notification_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Main method runs SpringApplication")
    void testMainMethod() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(eq(NotificationServiceApplication.class), any(String[].class)))
                    .thenReturn(null);

            assertDoesNotThrow(() -> NotificationServiceApplication.main(new String[]{}));
        }
    }
}
