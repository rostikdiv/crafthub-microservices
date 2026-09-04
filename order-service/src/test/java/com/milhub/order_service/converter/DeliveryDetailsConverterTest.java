package com.milhub.order_service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.order_service.dto.delivery.DeliveryDetailsDTO;
import com.milhub.order_service.entity.enums.DeliveryProvider;
import com.milhub.order_service.entity.enums.DeliveryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryDetailsConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeliveryDetailsConverter converter = new DeliveryDetailsConverter(objectMapper);

    @Test
    @DisplayName("convertToDatabaseColumn serializes object to json and handles null")
    void testConvertToDatabaseColumn() {
        assertNull(converter.convertToDatabaseColumn(null));

        DeliveryDetailsDTO dto = DeliveryDetailsDTO.builder()
                .provider(DeliveryProvider.NOVA_POSHTA)
                .type(DeliveryType.BRANCH)
                .cityRef("city-ref-1")
                .branchRef("branch-ref-2")
                .recipientEmail("user@milhub.com")
                .build();

        String json = converter.convertToDatabaseColumn(dto);
        assertNotNull(json);
        assertTrue(json.contains("city-ref-1"));
        assertTrue(json.contains("NOVA_POSHTA"));
    }

    @Test
    @DisplayName("convertToDatabaseColumn throws RuntimeException on serialization failure")
    void testConvertToDatabaseColumn_JsonException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        DeliveryDetailsConverter faultyConverter = new DeliveryDetailsConverter(mockMapper);

        when(mockMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});
        DeliveryDetailsDTO dto = DeliveryDetailsDTO.builder().build();

        assertThrows(RuntimeException.class, () -> faultyConverter.convertToDatabaseColumn(dto));
    }

    @Test
    @DisplayName("convertToEntityAttribute deserializes json to object and handles null/empty")
    void testConvertToEntityAttribute() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));

        String json = "{\"provider\":\"UKRPOSHTA\",\"type\":\"COURIER\",\"street\":\"Main St\",\"building\":\"10\"}";
        DeliveryDetailsDTO dto = converter.convertToEntityAttribute(json);

        assertNotNull(dto);
        assertEquals(DeliveryProvider.UKRPOSHTA, dto.provider());
        assertEquals(DeliveryType.COURIER, dto.type());
        assertEquals("Main St", dto.street());
        assertEquals("10", dto.building());
    }

    @Test
    @DisplayName("convertToEntityAttribute throws RuntimeException on deserialization failure")
    void testConvertToEntityAttribute_JsonException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        DeliveryDetailsConverter faultyConverter = new DeliveryDetailsConverter(mockMapper);

        when(mockMapper.readValue(anyString(), eq(DeliveryDetailsDTO.class)))
                .thenThrow(new JsonProcessingException("Deserialization failed") {});

        assertThrows(RuntimeException.class, () -> faultyConverter.convertToEntityAttribute("{\"bad\":\"json\"}"));
    }
}
