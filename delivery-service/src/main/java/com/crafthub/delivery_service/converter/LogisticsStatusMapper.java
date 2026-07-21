package com.crafthub.delivery_service.converter;

import com.crafthub.delivery_service.entity.DeliveryProvider;
import com.crafthub.delivery_service.entity.DeliveryStatus;
import org.springframework.stereotype.Component;

/**
 * Maps external statuses from various logistics providers (Nova Poshta, Ukrposhta, etc.)
 * to the internal DeliveryStatus enum.
 */
@Component
public class LogisticsStatusMapper {

    /**
     * Maps an external provider's tracking status string to the internal DeliveryStatus.
     *
     * @param provider The logistics provider
     * @param externalStatus The raw status string from the provider's API
     * @return The corresponding internal DeliveryStatus, or null if unmapped
     */
    public DeliveryStatus mapStatus(DeliveryProvider provider, String externalStatus) {
        if (externalStatus == null || externalStatus.isBlank()) {
            return null;
        }

        String status = externalStatus.trim().toLowerCase();

        return switch (provider) {
            case NOVA_POSHTA -> mapNovaPoshtaStatus(status);
            case UKRPOSHTA -> mapUkrposhtaStatus(status);
            case SELLER -> mapSellerStatus(status);
            default -> null; // Unknown provider
        };
    }

    private DeliveryStatus mapNovaPoshtaStatus(String status) {
        if (status.contains("очікується відправлення") || status.contains("створено електронну заявку")) {
            return DeliveryStatus.PREPARING;
        }
        if (status.contains("прийнято") || status.contains("прямує до міста") || status.contains("в дорозі")) {
            return DeliveryStatus.SHIPPED;
        }
        if (status.contains("прибув у відділення") || status.contains("очікує")) {
            return DeliveryStatus.SHIPPED;
        }
        if (status.contains("отримано") || status.contains("доставлено") || status.contains("одержано")) {
            return DeliveryStatus.DELIVERED;
        }
        if (status.contains("відмова") || status.contains("повернення") || status.contains("відправлення повертається")) {
            return DeliveryStatus.RETURNED;
        }
        if (status.contains("видалено") || status.contains("скасовано")) {
            return DeliveryStatus.CANCELLED;
        }
        return null;
    }

    private DeliveryStatus mapUkrposhtaStatus(String status) {
        if (status.contains("прийнято") || status.contains("відправлення прийняте")) {
            return DeliveryStatus.PREPARING;
        }
        if (status.contains("знаходиться в дорозі") || status.contains("відправлено") || status.contains("надходження")) {
            return DeliveryStatus.SHIPPED;
        }
        if (status.contains("вручено") || status.contains("доставлено")) {
            return DeliveryStatus.DELIVERED;
        }
        if (status.contains("повернення") || status.contains("відмова")) {
            return DeliveryStatus.RETURNED;
        }
        return null;
    }

    private DeliveryStatus mapSellerStatus(String status) {
        if (status.contains("готово до відправки")) {
            return DeliveryStatus.READY_TO_SHIP;
        }
        if (status.contains("в дорозі")) {
            return DeliveryStatus.SHIPPED;
        }
        if (status.contains("доставлено")) {
            return DeliveryStatus.DELIVERED;
        }
        return null;
    }
}
