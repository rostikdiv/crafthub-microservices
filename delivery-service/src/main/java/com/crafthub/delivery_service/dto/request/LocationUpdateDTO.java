package com.crafthub.delivery_service.dto.request;

import com.crafthub.delivery_service.entity.DeliveryProvider;

public record LocationUpdateDTO(
                String nameUkr,
                String region,
                DeliveryProvider provider) {
}