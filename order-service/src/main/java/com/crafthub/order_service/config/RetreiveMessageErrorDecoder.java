package com.crafthub.order_service.config;

import com.crafthub.order_service.exception.BusinessException;
import com.crafthub.order_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Feign error decoder to map HTTP error statuses from external services
 * to application-specific exceptions.
 */
public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = null;
        try (InputStream bodyIs = response.body().asInputStream()) {
            // Read error message from the external service response body
            if (bodyIs != null) {
                errorMessage = StreamUtils.copyToString(bodyIs, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            errorMessage = "Failed to process error response";
        }

        // Default error message if body is empty
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Unknown error from external service";
        }

        switch (response.status()) {
            case 400:
                // 400 Bad Request -> BusinessException (e.g., "Insufficient stock")
                return new BusinessException(errorMessage);
            case 404:
                // 404 Not Found -> ResourceNotFoundException (e.g., "Product not found")
                return new ResourceNotFoundException(errorMessage);
            default:
                // Pass other errors (500, 403) to the default decoder
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}