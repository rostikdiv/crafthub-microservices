package com.crafthub.order_service.converter;

import com.crafthub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Converter
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryDetailsConverter implements AttributeConverter<DeliveryDetailsDTO, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(DeliveryDetailsDTO attribute) {
        if (attribute == null) return null;
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error converting DeliveryDetailsDTO to JSON", e);
            throw new RuntimeException("Error converting delivery details to JSON", e);
        }
    }

    @Override
    public DeliveryDetailsDTO convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return null;
        try {
            return objectMapper.readValue(dbData, DeliveryDetailsDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Error reading JSON to DeliveryDetailsDTO", e);
            throw new RuntimeException("Error reading JSON delivery details", e);
        }
    }
}