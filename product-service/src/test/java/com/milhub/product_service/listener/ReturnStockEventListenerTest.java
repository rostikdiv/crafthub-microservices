package com.milhub.product_service.listener;

import com.milhub.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnStockEventListenerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ReturnStockEventListener returnStockEventListener;

    @Test
    @DisplayName("handleStockReturn: calls productService.restoreStock with correct args")
    void handleStockReturn_WhenValidEvent_ShouldRestoreStock() {
        UUID productId = UUID.randomUUID();
        ReturnStockEventListener.StockReturnEventDTO event =
                new ReturnStockEventListener.StockReturnEventDTO(productId, 5, "Order Cancelled");

        returnStockEventListener.handleStockReturn(event);

        verify(productService).restoreStock(productId, 5);
    }

    @Test
    @DisplayName("handleStockReturn: catches exception gracefully when restoreStock fails")
    void handleStockReturn_WhenServiceFails_ShouldCatchAndNotPropagate() {
        UUID productId = UUID.randomUUID();
        ReturnStockEventListener.StockReturnEventDTO event =
                new ReturnStockEventListener.StockReturnEventDTO(productId, 5, "Order Cancelled");

        doThrow(new RuntimeException("DB Connection Error")).when(productService).restoreStock(productId, 5);

        // Should complete without throwing exception
        returnStockEventListener.handleStockReturn(event);

        verify(productService).restoreStock(productId, 5);
    }
}
