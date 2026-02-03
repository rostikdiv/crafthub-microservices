package com.crafthub.notification_service.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryStatusChangedEvent(
        UUID orderId,
        String status,
        LocalDateTime timestamp
) {}