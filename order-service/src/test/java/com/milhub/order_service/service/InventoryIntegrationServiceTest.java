package com.milhub.order_service.service;

import com.milhub.order_service.dto.order.OrderItemRequestDTO;
import com.milhub.order_service.entity.Order;
import com.milhub.order_service.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryIntegrationServiceTest {

    @Mock
    private ProductIntegrationService productIntegrationService;

    @InjectMocks
    private InventoryIntegrationService inventoryIntegrationService;

    @Captor
    private ArgumentCaptor<List<OrderItemRequestDTO>> itemsCaptor;

    @Test
    @DisplayName("restoreStock calls productIntegrationService with mapped DTOs")
    void restoreStock_ShouldCallProductIntegrationService() {
        Order order = new Order();
        order.setId(UUID.randomUUID());

        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(3);
        order.setItems(List.of(item));

        inventoryIntegrationService.restoreStock(order);

        verify(productIntegrationService).restoreStock(itemsCaptor.capture());
        List<OrderItemRequestDTO> restoredItems = itemsCaptor.getValue();

        assertThat(restoredItems).hasSize(1);
        assertThat(restoredItems.get(0).productId()).isEqualTo(item.getProductId());
        assertThat(restoredItems.get(0).quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("restoreStock returns immediately when order items is null or empty")
    void restoreStock_NullOrEmptyItems() {
        Order orderNullItems = new Order();
        orderNullItems.setItems(null);
        inventoryIntegrationService.restoreStock(orderNullItems);

        Order orderEmptyItems = new Order();
        orderEmptyItems.setItems(Collections.emptyList());
        inventoryIntegrationService.restoreStock(orderEmptyItems);

        verifyNoInteractions(productIntegrationService);
    }

    @Test
    @DisplayName("restoreStock handles exception from productIntegrationService gracefully")
    void restoreStock_ExceptionHandled() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(1);
        order.setItems(List.of(item));

        doThrow(new RuntimeException("Downstream fail")).when(productIntegrationService).restoreStock(anyList());

        assertDoesNotThrow(() -> inventoryIntegrationService.restoreStock(order));
    }
}
