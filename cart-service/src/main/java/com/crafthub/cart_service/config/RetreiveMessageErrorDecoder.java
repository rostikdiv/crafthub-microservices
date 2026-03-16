package com.crafthub.cart_service.config;

import com.crafthub.cart_service.exception.BusinessException;
import com.crafthub.cart_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Feign error decoder that maps external service error responses to local
 * exceptions.
 */
public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = null;
        try (InputStream bodyIs = response.body().asInputStream()) {
            // Read the error message body from the external service
            if (bodyIs != null) {
                errorMessage = StreamUtils.copyToString(bodyIs, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            errorMessage = "Failed to process error response";
        }

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
                // Other statuses (500, 403, etc.) use default Feign behavior
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}