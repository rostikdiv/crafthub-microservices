package com.crafthub.delivery_service.dto.request;

import com.crafthub.delivery_service.dto.external.DeliveryDetailsDTO;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReturnShipmentRequestDTO(
        @NotNull UUID orderId,
        @NotNull DeliveryDetailsDTO returnAddress, // Куди повертати (склад) або звідки забирати (від клієнта) -
                                                   // залежить від логіки. Зазвичай: адреса клієнта (звідки кур'єр
                                                   // забере) або відділення.
        Double weight) {
}
