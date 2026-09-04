package com.milhub.product_service.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.milhub.product_service.dto.event.RefundApprovedEventDTO;
import com.milhub.product_service.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundEventListenerTest {

    @Mock
    private ProductService productService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RefundEventListener refundEventListener;

    @Test
    @DisplayName("handleRefundApprovedEvent: successfully parses JSON and calls restoreStock")
    void handleRefundApprovedEvent_WhenValidMessage_ShouldRestoreStock() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        RefundApprovedEventDTO dto = new RefundApprovedEventDTO(orderId, productId, 3, "Customer return");
        String message = objectMapper.writeValueAsString(dto);

        refundEventListener.handleRefundApprovedEvent(message);

        verify(productService).restoreStock(productId, 3);
    }

    @Test
    @DisplayName("handleRefundApprovedEvent: catches exception and does not propagate when invalid message")
    void handleRefundApprovedEvent_WhenInvalidMessage_ShouldCatchAndNotPropagate() {
        String invalidJson = "{ invalid-json }";

        refundEventListener.handleRefundApprovedEvent(invalidJson);

        verify(productService, never()).restoreStock(any(), any());
    }

    @Test
    @DisplayName("handleRefundApprovedEvent: catches exception when productService throws exception")
    void handleRefundApprovedEvent_WhenServiceFails_ShouldCatchGracefully() throws Exception {
        UUID productId = UUID.randomUUID();
        RefundApprovedEventDTO dto = new RefundApprovedEventDTO(UUID.randomUUID(), productId, 2, "Defect");
        String message = objectMapper.writeValueAsString(dto);

        doThrow(new RuntimeException("Database error")).when(productService).restoreStock(productId, 2);

        // Should not throw exception
        refundEventListener.handleRefundApprovedEvent(message);

        verify(productService).restoreStock(productId, 2);
    }
}
