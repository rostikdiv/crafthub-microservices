package com.crafthub.order_service.service;

import com.crafthub.order_service.dto.order.OrderItemRequestDTO;
import com.crafthub.order_service.entity.Order;
import com.crafthub.order_service.entity.OrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryIntegrationServiceTest {

    @Mock
    private ProductIntegrationService productIntegrationService;

    @InjectMocks
    private InventoryIntegrationService inventoryIntegrationService;

    @Captor
    private ArgumentCaptor<List<OrderItemRequestDTO>> itemsCaptor;

    @Test
    void restoreStock_ShouldCallProductIntegrationService() {
        // Arrange
        Order order = new Order();
        order.setId(UUID.randomUUID());
        
        OrderItem item = new OrderItem();
        item.setProductId(UUID.randomUUID());
        item.setQuantity(3);
        order.setItems(List.of(item));

        // Act
        inventoryIntegrationService.restoreStock(order);

        // Assert
        verify(productIntegrationService).restoreStock(itemsCaptor.capture());
        List<OrderItemRequestDTO> restoredItems = itemsCaptor.getValue();
        
        assertThat(restoredItems).hasSize(1);
        assertThat(restoredItems.get(0).productId()).isEqualTo(item.getProductId());
        assertThat(restoredItems.get(0).quantity()).isEqualTo(3);
    }
}
