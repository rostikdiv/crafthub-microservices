package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.address.AddressDTO;
import com.crafthub.user_service.dto.address.SellerPointDTO;
import com.crafthub.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me") // ✅ Всі методи стосуються "Мене" (поточного юзера)
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // === АДРЕСИ ПОКУПЦЯ ===

    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> addAddress(@RequestBody AddressDTO dto) {
        // ID юзера не передається, він береться з токена
        return ResponseEntity.ok(addressService.saveAddress(dto));
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    // === ТОЧКИ ПРОДАВЦЯ ===

    @PostMapping("/seller-points")
    public ResponseEntity<SellerPointDTO> addSellerPoint(@RequestBody SellerPointDTO dto) {
        return ResponseEntity.ok(addressService.addSellerPoint(dto));
    }

    @GetMapping("/seller-points")
    public ResponseEntity<List<SellerPointDTO>> getMySellerPoints() {
        return ResponseEntity.ok(addressService.getMySellerPoints());
    }
}