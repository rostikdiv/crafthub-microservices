package com.milhub.api_gateway.controller;

import com.milhub.api_gateway.service.SystemWarmupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller providing a warmup endpoint (/api/v1/system/warmup) for the frontend
 * to trigger concurrent wake-up requests to all Cloud Run microservices.
 * Only enabled under the "cloud" profile.
 */
@RestController
@RequestMapping("/api/v1/system")
@Profile("cloud")
@RequiredArgsConstructor
public class SystemWarmupController {

    private final SystemWarmupService systemWarmupService;

    @GetMapping("/warmup")
    public Mono<SystemWarmupService.WarmupResponse> warmupSystem() {
        return systemWarmupService.warmupAllServices();
    }
}
