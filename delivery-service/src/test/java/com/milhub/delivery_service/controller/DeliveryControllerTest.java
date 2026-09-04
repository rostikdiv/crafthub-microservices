package com.milhub.delivery_service.controller;

import com.milhub.delivery_service.dto.request.WebhookDTO;
import com.milhub.delivery_service.dto.response.ShipmentResponseDTO;
import com.milhub.delivery_service.entity.DeliveryProvider;
import com.milhub.delivery_service.entity.DeliveryStatus;
import com.milhub.delivery_service.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private DeliveryController controller;

    @Test
    void getAllShipments_ShouldReturnShipments() {
        ShipmentResponseDTO dto = ShipmentResponseDTO.builder().id(UUID.randomUUID()).build();
        when(shipmentService.getAllShipments()).thenReturn(List.of(dto));

        ResponseEntity<List<ShipmentResponseDTO>> response = controller.getAllShipments();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        verify(shipmentService).getAllShipments();
    }

    @Test
    void getByOrderId_ShouldReturnShipment() {
        UUID orderId = UUID.randomUUID();
        ShipmentResponseDTO dto = ShipmentResponseDTO.builder().orderId(orderId).build();
        when(shipmentService.getShipmentByOrderId(orderId)).thenReturn(dto);

        ResponseEntity<ShipmentResponseDTO> response = controller.getByOrderId(orderId);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(dto);
        verify(shipmentService).getShipmentByOrderId(orderId);
    }

    @Test
    void updateStatus_ShouldCallService() {
        UUID shipmentId = UUID.randomUUID();

        ResponseEntity<String> response = controller.updateStatus(shipmentId, DeliveryStatus.SHIPPED);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Status updated to SHIPPED");
        verify(shipmentService).updateShipmentStatus(shipmentId, DeliveryStatus.SHIPPED);
    }

    @Test
    void processExternalWebhook_ShouldCallService() {
        WebhookDTO dto = new WebhookDTO(DeliveryProvider.NOVA_POSHTA, "20450000000001", "Відправлення отримано");

        ResponseEntity<String> response = controller.processExternalWebhook(dto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Webhook processed successfully");
        verify(shipmentService).processExternalWebhook(DeliveryProvider.NOVA_POSHTA, "20450000000001", "Відправлення отримано");
    }
}
