package com.crafthub.api_gateway.controller;

import com.crafthub.api_gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gateway")
@RequiredArgsConstructor
public class GatewayTestController {

    private final JwtUtil jwtUtil;

    @GetMapping("/test-token")
    public ResponseEntity<String> testToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid Authorization header");
        }

        String token = authHeader.substring(7);
        boolean isValid = jwtUtil.isTokenValid(token);

        return ResponseEntity.ok("Token is valid: " + isValid);
    }
}