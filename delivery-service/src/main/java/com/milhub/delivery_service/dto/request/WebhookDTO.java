package com.milhub.delivery_service.dto.request;

import com.milhub.delivery_service.entity.DeliveryProvider;

public record WebhookDTO(
        DeliveryProvider provider,
        String trackingNumber,
        String rawStatus
) {}
