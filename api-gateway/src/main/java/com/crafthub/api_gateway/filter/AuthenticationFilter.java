package com.crafthub.api_gateway.filter;

import com.crafthub.api_gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;

/**
 * Custom gateway filter for authenticating incoming requests via JWT.
 * Extracts claims and forwards them to downstream microservices as HTTP
 * headers.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private RouteValidator validator;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthenticationFilter() {
        super(Config.class);
    }

    /**
     * Applies the authentication logic to the gateway exchange.
     */
    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Check if the route requires authentication
            if (validator.isSecured.test(request)) {
                System.out.println("SECURED REQUEST: " + request.getURI());

                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    System.out.println("MISSING AUTH HEADER");
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authorization header");
                }

                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authHeader = authHeader.substring(7);
                } else {
                    System.out.println("INVALID AUTH HEADER FORMAT");
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header format");
                }

                try {
                    // Validates the token and extracts claims
                    jwtUtil.validateToken(authHeader);

                    Claims claims = jwtUtil.getAllClaimsFromToken(authHeader);
                    String userId = claims.get("id", String.class);
                    String role = claims.get("role", String.class);
                    String email = claims.getSubject();

                    List<String> permissions = claims.get("permissions", List.class);
                    if (permissions == null) {
                        permissions = new ArrayList<>();
                    } else {
                        permissions = new ArrayList<>(permissions);
                    }

                    // Prepend ROLE_ to the user role for Spring Security compatibility downstream
                    if (role != null && !role.isEmpty()) {
                        permissions.add("ROLE_" + role);
                    }

                    String permissionsStr = String.join(",", permissions);
                    String isVerified = String.valueOf(claims.get("isVerified"));

                    System.out.println("TOKEN VALID. User: " + email + ", Role: " + role);
                    System.out.println("ADDING HEADERS: X-User-Id=" + userId + ", X-User-Permissions (length="
                            + permissionsStr.length() + ")");

                    // Mutate the request to include user identity headers
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            .header("X-User-Email", email)
                            .header("X-User-Permissions", permissionsStr)
                            .header("X-User-Is-Verified", isVerified)
                            .build();

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());

                } catch (Exception e) {
                    System.out.println("UNAUTHORIZED: " + e.getMessage());
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized access");
                }
            } else {
                System.out.println("OPEN REQUEST: " + request.getURI());
            }

            return chain.filter(exchange);
        });
    }

    public static class Config {
        // Configuration properties can be added here
    }
}