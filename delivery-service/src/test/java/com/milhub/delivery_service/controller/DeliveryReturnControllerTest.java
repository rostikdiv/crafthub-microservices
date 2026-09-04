package com.milhub.delivery_service.controller;

import com.milhub.delivery_service.dto.request.ReturnShipmentRequestDTO;
import com.milhub.delivery_service.dto.response.ReturnShipmentResponseDTO;
import com.milhub.delivery_service.service.ShipmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryReturnControllerTest {

    @Mock
    private ShipmentService shipmentService;

    @InjectMocks
    private DeliveryReturnController controller;

    @Test
    void createReturnShipment_ShouldCallService() {
        ReturnShipmentRequestDTO request = new ReturnShipmentRequestDTO(UUID.randomUUID(), null, 1.0);
        ReturnShipmentResponseDTO responseDTO = new ReturnShipmentResponseDTO(UUID.randomUUID(), "RET-123", BigDecimal.valueOf(70.0));
        when(shipmentService.createReturnShipment(request)).thenReturn(responseDTO);

        ResponseEntity<ReturnShipmentResponseDTO> response = controller.createReturnShipment(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(shipmentService).createReturnShipment(request);
    }
}
