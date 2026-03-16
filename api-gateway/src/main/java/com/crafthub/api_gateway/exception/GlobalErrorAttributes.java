package com.crafthub.api_gateway.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Custom error attribute provider to standardize error responses across the API
 * Gateway.
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> map = super.getErrorAttributes(request, options);

        Throwable error = getError(request);

        map.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        map.put("path", request.path());
        map.put("message", error.getMessage());

        // Determine the HTTP status code based on the exception type
        int status = (int) map.getOrDefault("status", 500);

        if (error instanceof org.springframework.web.server.ResponseStatusException) {
            status = ((org.springframework.web.server.ResponseStatusException) error).getStatusCode().value();
        } else if (error.getMessage() != null
                && (error.getMessage().contains("Unauthorized") || error.getMessage().contains("Jwt"))) {
            status = HttpStatus.UNAUTHORIZED.value();
            map.put("error", "Unauthorized");
        } else if (error instanceof java.net.ConnectException) {
            status = HttpStatus.SERVICE_UNAVAILABLE.value();
            map.put("error", "Service Unavailable");
            map.put("message", "Microservice is down or unreachable");
        }

        map.put("status", status);

        // Remove technical/internal metadata from the response
        map.remove("requestId");
        map.remove("trace");

        return map;
    }
}