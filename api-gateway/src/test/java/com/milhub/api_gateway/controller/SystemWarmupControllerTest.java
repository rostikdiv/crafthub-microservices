package com.milhub.api_gateway.controller;

import com.milhub.api_gateway.service.SystemWarmupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemWarmupController Tests")
class SystemWarmupControllerTest {

    @Mock
    private SystemWarmupService systemWarmupService;

    @InjectMocks
    private SystemWarmupController controller;

    @Test
    void testWarmupSystem() {
        SystemWarmupService.WarmupResponse mockResponse = SystemWarmupService.WarmupResponse.builder()
                .status("UP")
                .services(Map.of("user-service", "WARMED_UP"))
                .build();

        when(systemWarmupService.warmupAllServices()).thenReturn(Mono.just(mockResponse));

        SystemWarmupService.WarmupResponse result = controller.warmupSystem().block();

        assertNotNull(result);
        assertEquals("UP", result.getStatus());
        assertEquals("WARMED_UP", result.getServices().get("user-service"));
        verify(systemWarmupService, times(1)).warmupAllServices();
    }
}
