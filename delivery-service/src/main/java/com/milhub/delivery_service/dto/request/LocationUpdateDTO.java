package com.milhub.delivery_service.dto.request;

import com.milhub.delivery_service.entity.DeliveryProvider;

public record LocationUpdateDTO(
                String nameUkr,
                String region,
                DeliveryProvider provider) {
}