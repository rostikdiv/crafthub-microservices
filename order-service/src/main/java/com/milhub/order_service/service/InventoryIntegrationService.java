package com.milhub.order_service.service;

import com.milhub.order_service.dto.order.OrderItemRequestDTO;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderItem;
import com.milhub.order_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryIntegrationService {

    private final ProductIntegrationService productIntegrationService;

    public void restoreStock(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        log.info("Restoring stock for Order {}", order.getId());
        List<OrderItemRequestDTO> itemsToRestore = order.getItems().stream()
                .map(item -> new OrderItemRequestDTO(item.getProductId(), item.getQuantity()))
                .toList();

        try {
            productIntegrationService.restoreStock(itemsToRestore);
            log.info("Stock restored successfully for Order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to restore stock for Order {}. Manual intervention may be required.", order.getId(), e);
        }
    }
    
    // Additional inventory related logic can go here (e.g., reserve stock)
}
