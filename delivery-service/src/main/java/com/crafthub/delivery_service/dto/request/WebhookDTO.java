package com.crafthub.delivery_service.dto.request;

import com.crafthub.delivery_service.entity.DeliveryProvider;

public record WebhookDTO(
        DeliveryProvider provider,
        String trackingNumber,
        String rawStatus
) {}
