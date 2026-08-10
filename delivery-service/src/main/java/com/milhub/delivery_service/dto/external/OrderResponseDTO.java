package com.milhub.delivery_service.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

/**
 * Data Transfer Object representing an order retrieved from Order Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponseDTO {
    private UUID id;
    private UUID userId;
    // Delivery information object (automatically parsed from JSON)
    private DeliveryDetailsDTO deliveryInfo;
}