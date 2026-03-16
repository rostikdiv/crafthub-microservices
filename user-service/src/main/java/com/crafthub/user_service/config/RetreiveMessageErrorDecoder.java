package com.crafthub.user_service.config;

import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Custom Feign error decoder that attempts to retrieve and propagate error
 * messages from external services.
 */
public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = null;
        try (InputStream bodyIs = response.body().asInputStream()) {
            // Read error text returned by the remote service
            if (bodyIs != null) {
                errorMessage = StreamUtils.copyToString(bodyIs, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            errorMessage = "Failed to process error response";
        }

        // Set default if message is empty
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Unknown error from external service";
        }

        switch (response.status()) {
            case 400:
                // 400 Bad Request maps to local BusinessException
                return new BusinessException(errorMessage);
            case 404:
                // 404 Not Found maps to local ResourceNotFoundException
                return new ResourceNotFoundException(errorMessage);
            default:
                // Other errors (500, 403, etc.) are handled by the default decoder
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}