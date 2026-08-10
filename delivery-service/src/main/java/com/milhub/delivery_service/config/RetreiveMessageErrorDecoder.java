package com.milhub.delivery_service.config;

import com.milhub.delivery_service.exception.BusinessException;
import com.milhub.delivery_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Custom Feign error decoder that translates HTTP error responses from external
 * microservices into local application exceptions.
 */
public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = null;
        try (InputStream bodyIs = response.body().asInputStream()) {
            // Read error message returned by the external service
            if (bodyIs != null) {
                errorMessage = StreamUtils.copyToString(bodyIs, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            errorMessage = "Failed to process error response";
        }

        // Set default message if response body is empty
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Unknown error from external service";
        }

        switch (response.status()) {
            case 400:
                // 400 Bad Request -> BusinessException
                return new BusinessException(errorMessage);
            case 404:
                // 404 Not Found -> ResourceNotFoundException
                return new ResourceNotFoundException(errorMessage);
            default:
                // All other errors (500, 403, etc.) are handled by the default decoder
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}