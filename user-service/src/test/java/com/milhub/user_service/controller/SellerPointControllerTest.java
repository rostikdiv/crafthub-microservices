package com.milhub.user_service.controller;

import com.milhub.user_service.dto.address.SellerPointDTO;
import com.milhub.user_service.service.SellerPointService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerPointControllerTest {

    @Mock
    private SellerPointService pointService;

    @InjectMocks
    private SellerPointController controller;

    private final UUID userId = UUID.randomUUID();
    private final UUID pointId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPoint_ShouldCallService() {
        SellerPointDTO inputDto = new SellerPointDTO(null, "Point 1", null, "Kyiv", "Kyivska", "Street", "1", null, null, "123", null);
        SellerPointDTO outputDto = new SellerPointDTO(pointId, "Point 1", null, "Kyiv", "Kyivska", "Street", "1", null, null, "123", null);

        when(pointService.createPoint(userId, inputDto)).thenReturn(outputDto);

        ResponseEntity<SellerPointDTO> response = controller.createPoint(inputDto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(outputDto);
        verify(pointService).createPoint(userId, inputDto);
    }

    @Test
    void getMyPoints_ShouldCallService() {
        when(pointService.getMyPoints(userId)).thenReturn(List.of());

        ResponseEntity<List<SellerPointDTO>> response = controller.getMyPoints();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(pointService).getMyPoints(userId);
    }

    @Test
    void updatePoint_ShouldCallService() {
        SellerPointDTO inputDto = new SellerPointDTO(pointId, "Updated", null, "Kyiv", "Kyivska", "Street", "1", null, null, "123", null);

        when(pointService.updatePoint(userId, pointId, inputDto)).thenReturn(inputDto);

        ResponseEntity<SellerPointDTO> response = controller.updatePoint(pointId, inputDto);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(inputDto);
        verify(pointService).updatePoint(userId, pointId, inputDto);
    }

    @Test
    void deletePoint_ShouldCallService() {
        ResponseEntity<Void> response = controller.deletePoint(pointId);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(pointService).deletePoint(userId, pointId);
    }
}
