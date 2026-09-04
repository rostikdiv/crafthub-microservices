package com.milhub.user_service.controller;

import com.milhub.user_service.dto.address.AddressDTO;
import com.milhub.user_service.dto.address.SellerPointDTO;
import com.milhub.user_service.entity.enums.DeliveryProvider;
import com.milhub.user_service.entity.enums.DeliveryType;
import com.milhub.user_service.service.AddressService;
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
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController controller;

    @Test
    void addAddress_ShouldCallService() {
        AddressDTO input = new AddressDTO(
                null, "Home", DeliveryProvider.NOVA_POSHTA, DeliveryType.BRANCH,
                "ref-1", "Kyiv", "Kyivska", "br-1", "Branch 1",
                null, null, null, null
        );
        when(addressService.saveAddress(input)).thenReturn(input);

        ResponseEntity<AddressDTO> response = controller.addAddress(input);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(input);
        verify(addressService).saveAddress(input);
    }

    @Test
    void getMyAddresses_ShouldCallService() {
        when(addressService.getMyAddresses()).thenReturn(List.of());

        ResponseEntity<List<AddressDTO>> response = controller.getMyAddresses();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(addressService).getMyAddresses();
    }

    @Test
    void addSellerPoint_ShouldCallService() {
        SellerPointDTO input = new SellerPointDTO(
                null, "Point 1", null, "Kyiv", "Kyivska", "Street", "1", null, null, "123", null
        );
        when(addressService.addSellerPoint(input)).thenReturn(input);

        ResponseEntity<SellerPointDTO> response = controller.addSellerPoint(input);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(input);
        verify(addressService).addSellerPoint(input);
    }

    @Test
    void getMySellerPoints_ShouldCallService() {
        when(addressService.getMySellerPoints()).thenReturn(List.of());

        ResponseEntity<List<SellerPointDTO>> response = controller.getMySellerPoints();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(addressService).getMySellerPoints();
    }
}
