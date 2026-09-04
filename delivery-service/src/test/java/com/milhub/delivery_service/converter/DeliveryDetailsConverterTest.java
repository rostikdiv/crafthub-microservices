package com.milhub.delivery_service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.delivery_service.dto.external.DeliveryDetailsDTO;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.DeliveryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryDetailsConverterTest {

    @Mock
    private ObjectMapper mockObjectMapper;

    private DeliveryDetailsConverter realConverter;
    private DeliveryDetailsConverter mockConverter;

    @BeforeEach
    void setUp() {
        realConverter = new DeliveryDetailsConverter(new ObjectMapper());
        mockConverter = new DeliveryDetailsConverter(mockObjectMapper);
    }

    @Test
    void convertToDatabaseColumn_WhenAttributeIsNull_ShouldReturnNull() {
        String result = realConverter.convertToDatabaseColumn(null);
        assertThat(result).isNull();
    }

    @Test
    void convertToDatabaseColumn_WhenAttributeIsValid_ShouldReturnJson() {
        DeliveryDetailsDTO dto = new DeliveryDetailsDTO(
                DeliveryProvider.NOVA_POSHTA, DeliveryType.BRANCH,
                "ref-1", "Kyiv", "Kyivska", "br-1", "Branch 1",
                "Khreshchatyk", "1", "10", "01001", UUID.randomUUID(), "Pickup", "Call before"
        );

        String json = realConverter.convertToDatabaseColumn(dto);

        assertThat(json).isNotNull();
        assertThat(json).contains("NOVA_POSHTA");
        assertThat(json).contains("Kyiv");
    }

    @Test
    void convertToDatabaseColumn_WhenJsonProcessingException_ShouldThrowRuntimeException() throws Exception {
        DeliveryDetailsDTO dto = new DeliveryDetailsDTO(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(mockObjectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Serialization failed") {});

        RuntimeException ex = assertThrows(RuntimeException.class, () -> mockConverter.convertToDatabaseColumn(dto));
        assertThat(ex.getMessage()).contains("Error converting delivery details to JSON");
    }

    @Test
    void convertToEntityAttribute_WhenDbDataIsNull_ShouldReturnNull() {
        DeliveryDetailsDTO result = realConverter.convertToEntityAttribute(null);
        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttribute_WhenDbDataIsEmpty_ShouldReturnNull() {
        DeliveryDetailsDTO result = realConverter.convertToEntityAttribute("");
        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttribute_WhenValidJson_ShouldReturnDto() {
        String json = "{\"provider\":\"NOVA_POSHTA\",\"type\":\"BRANCH\",\"cityName\":\"Kyiv\"}";

        DeliveryDetailsDTO result = realConverter.convertToEntityAttribute(json);

        assertThat(result).isNotNull();
        assertThat(result.provider()).isEqualTo(DeliveryProvider.NOVA_POSHTA);
        assertThat(result.type()).isEqualTo(DeliveryType.BRANCH);
        assertThat(result.cityName()).isEqualTo("Kyiv");
    }

    @Test
    void convertToEntityAttribute_WhenJsonProcessingException_ShouldThrowRuntimeException() throws Exception {
        when(mockObjectMapper.readValue(eq("bad-json"), eq(DeliveryDetailsDTO.class)))
                .thenThrow(new JsonProcessingException("Deserialization failed") {});

        RuntimeException ex = assertThrows(RuntimeException.class, () -> mockConverter.convertToEntityAttribute("bad-json"));
        assertThat(ex.getMessage()).contains("Error reading JSON delivery details");
    }
}
