package com.milhub.api_gateway.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for warming up all downstream microservices in GCP Cloud Run
 * when running under the "cloud" profile.
 * Sends parallel non-blocking requests to /actuator/health of all services to prevent cold starts.
 */
@Service
@Profile("cloud")
@Slf4j
public class SystemWarmupService {

    private final WebClient webClient;

    @Value("${USER_SERVICE_URL:}")
    private String userServiceUrl;

    @Value("${PRODUCT_SERVICE_URL:}")
    private String productServiceUrl;

    @Value("${ORDER_SERVICE_URL:}")
    private String orderServiceUrl;

    @Value("${CART_SERVICE_URL:}")
    private String cartServiceUrl;

    @Value("${PAYMENT_SERVICE_URL:}")
    private String paymentServiceUrl;

    @Value("${DELIVERY_SERVICE_URL:}")
    private String deliveryServiceUrl;

    @Value("${NOTIFICATION_SERVICE_URL:}")
    private String notificationServiceUrl;

    public SystemWarmupService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WarmupResponse {
        private String status;
        private Map<String, String> services;
    }

    /**
     * Automatic single-shot warmup trigger on API Gateway application startup (cloud profile only).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupWarmup() {
        log.info("[CLOUD WARMUP] Triggering automatic initial warmup call to all downstream microservices...");
        warmupAllServices().subscribe(
                result -> log.info("[CLOUD WARMUP] Startup warmup completed successfully: {}", result),
                error -> log.warn("[CLOUD WARMUP] Startup warmup completed with errors: {}", error.getMessage())
        );
    }

    /**
     * Executes parallel health pings to all configured downstream microservices.
     *
     * @return Mono containing WarmupResponse for frontend banner status.
     */
    public Mono<WarmupResponse> warmupAllServices() {
        Map<String, String> serviceUrls = Map.of(
                "user-service", userServiceUrl,
                "product-service", productServiceUrl,
                "order-service", orderServiceUrl,
                "cart-service", cartServiceUrl,
                "payment-service", paymentServiceUrl,
                "delivery-service", deliveryServiceUrl,
                "notification-service", notificationServiceUrl
        );

        List<Mono<Map.Entry<String, String>>> pingMonos = serviceUrls.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> pingService(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return Flux.merge(pingMonos)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(servicesMap -> WarmupResponse.builder()
                        .status("UP")
                        .services(servicesMap)
                        .build());
    }

    private Mono<Map.Entry<String, String>> pingService(String serviceName, String baseUrl) {
        String healthUrl = baseUrl.endsWith("/") ? baseUrl + "actuator/health" : baseUrl + "/actuator/health";

        return webClient.get()
                .uri(healthUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .retry(2)
                .map(response -> Map.<String, String>entry(serviceName, "WARMED_UP"))
                .onErrorResume(ex -> {
                    log.warn("[CLOUD WARMUP] Warmup ping for {} at {} failed: {}", serviceName, healthUrl, ex.getMessage());
                    return Mono.just(Map.<String, String>entry(serviceName, "WARMUP_ATTEMPTED_ERR: " + ex.getMessage()));
                });
    }
}
