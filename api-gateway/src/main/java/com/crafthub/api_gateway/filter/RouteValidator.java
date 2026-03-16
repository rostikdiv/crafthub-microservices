package com.crafthub.api_gateway.filter;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Validator responsible for determining whether a given request requires
 * authentication.
 */
@Component
public class RouteValidator {

    /**
     * List of endpoints that are always open, regardless of the HTTP method.
     */
    public static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/delivery/locations",
            "/eureka");

    /**
     * Predicate that evaluates if a request is secured (requires JWT
     * authentication).
     */
    public Predicate<ServerHttpRequest> isSecured = request -> {
        String path = request.getURI().getPath();

        // Step 1: Check against explicitly open endpoints
        if (openApiEndpoints.stream().anyMatch(path::contains)) {
            return false;
        }

        // Step 2: Public access for specific GET requests (e.g., viewing products,
        // categories, or seller list for filters)
        if (request.getMethod().equals(HttpMethod.GET)) {
            if (path.contains("/api/v1/products") ||
                path.contains("/api/v1/categories") ||
                path.contains("/api/v1/sellers") ||
                path.contains("/api/v1/seller-reviews")) {
                return false;
            }
        }

        // Step 3: All other routes are secured by default
        return true;
    };
}