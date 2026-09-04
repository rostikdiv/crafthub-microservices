package com.milhub.api_gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemWarmupService Tests")
class SystemWarmupServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private ClientResponse clientResponse;

    private SystemWarmupService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        service = new SystemWarmupService(webClientBuilder);

        // Inject URLs: some with trailing slash, some without, some blank to test filter
        ReflectionTestUtils.setField(service, "userServiceUrl", "https://user-service.milhub.ua/");
        ReflectionTestUtils.setField(service, "productServiceUrl", "https://product-service.milhub.ua");
        ReflectionTestUtils.setField(service, "orderServiceUrl", "https://order-service.milhub.ua/");
        ReflectionTestUtils.setField(service, "cartServiceUrl", "https://cart-service.milhub.ua");
        ReflectionTestUtils.setField(service, "paymentServiceUrl", "https://payment-service.milhub.ua");
        ReflectionTestUtils.setField(service, "deliveryServiceUrl", ""); // blank to test filter
        ReflectionTestUtils.setField(service, "notificationServiceUrl", "   "); // whitespace to test isBlank filter
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWarmupAllServices_Success() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);

        when(requestHeadersSpec.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<Map.Entry<String, String>>> callback = invocation.getArgument(0);
            return callback.apply(clientResponse);
        });

        SystemWarmupService.WarmupResponse response = service.warmupAllServices().block();

        assertNotNull(response);
        assertEquals("UP", response.getStatus());
        assertEquals(5, response.getServices().size());
        assertEquals("WARMED_UP", response.getServices().get("user-service"));
        assertEquals("WARMED_UP", response.getServices().get("product-service"));
        assertNull(response.getServices().get("delivery-service"));
        assertNull(response.getServices().get("notification-service"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testWarmupAllServices_PingFails_RecoversGracefully() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any())).thenReturn(Mono.error(new RuntimeException("Network down")));

        SystemWarmupService.WarmupResponse response = service.warmupAllServices().block();

        assertNotNull(response);
        assertEquals("UP", response.getStatus());
        assertEquals(5, response.getServices().size());
        assertEquals("WARMED_UP", response.getServices().get("user-service"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOnStartupWarmup_Success() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);
        when(requestHeadersSpec.exchangeToMono(any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<Map.Entry<String, String>>> callback = invocation.getArgument(0);
            return callback.apply(clientResponse);
        });

        assertDoesNotThrow(() -> service.onStartupWarmup());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOnStartupWarmup_ErrorFlow() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        // Force an uncaught error after onErrorResume by returning error
        when(requestHeadersSpec.exchangeToMono(any())).thenReturn(Mono.error(new RuntimeException("Fatal failure")));

        assertDoesNotThrow(() -> service.onStartupWarmup());
    }

    @Test
    void testWarmupResponseDtoCoverage() {
        SystemWarmupService.WarmupResponse dto1 = new SystemWarmupService.WarmupResponse();
        dto1.setStatus("UP");
        dto1.setServices(Map.of("test", "WARMED_UP"));

        SystemWarmupService.WarmupResponse dto2 = new SystemWarmupService.WarmupResponse("UP", Map.of("test", "WARMED_UP"));
        SystemWarmupService.WarmupResponse dto3 = SystemWarmupService.WarmupResponse.builder()
                .status("UP")
                .services(Map.of("test", "WARMED_UP"))
                .build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertEquals(dto1.toString(), dto2.toString());
        assertEquals(dto1, dto3);
    }
}
