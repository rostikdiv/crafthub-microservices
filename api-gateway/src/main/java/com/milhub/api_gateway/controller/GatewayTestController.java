package com.milhub.api_gateway.controller;

import com.milhub.api_gateway.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller used for troubleshooting and verifying gateway-level
 * authentication.
 */
@RestController
@RequestMapping("/api/v1/gateway")
public class GatewayTestController {

    private final JwtUtil jwtUtil;

    public GatewayTestController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        System.out.println("✅ GATEWAY TEST CONTROLLER LOADED - VERSION 2.0");
    }

    /**
     * Diagnostic endpoint to verify if a given Bearer token is valid according to
     * the Gateway's JwtUtil.
     */
    @GetMapping("/test-token")
    public ResponseEntity<String> testToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid Authorization header");
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtUtil.isTokenValid(token);

        return ResponseEntity.ok("Token is valid: " + isValid);
    }

    /**
     * Simple health check endpoint for the gateway.
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API Gateway service is operational.");
    }
}