package com.crafthub.delivery_service.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponseDTO {
    private UUID id;
    private UUID userId;
    // Ми приймаємо це поле як об'єкт (Spring автоматично розпарсить JSON)
    private DeliveryDetailsDTO deliveryInfo;
}