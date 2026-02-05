package com.crafthub.user_service.config; // ⚠️ Змініть пакет під конкретний сервіс!

import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class RetreiveMessageErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String errorMessage = null;
        try (InputStream bodyIs = response.body().asInputStream()) {
            // Читаємо текст помилки, яку повернув інший сервіс
            if (bodyIs != null) {
                errorMessage = StreamUtils.copyToString(bodyIs, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            errorMessage = "Failed to process error response";
        }

        // Якщо повідомлення порожнє, ставимо дефолтне
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Unknown error from external service";
        }

        switch (response.status()) {
            case 400:
                // 400 Bad Request -> BusinessException
                // Наприклад: "Недостатньо товару на складі"
                return new BusinessException(errorMessage);
            case 404:
                // 404 Not Found -> ResourceNotFoundException
                // Наприклад: "Product not found"
                return new ResourceNotFoundException(errorMessage);
            default:
                // Всі інші помилки (500, 403) обробляються стандартно
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}