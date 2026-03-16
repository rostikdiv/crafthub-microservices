package com.crafthub.user_service.controller;

import com.crafthub.user_service.dto.address.AddressDTO;
import com.crafthub.user_service.dto.address.SellerPointDTO;
import com.crafthub.user_service.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing user-specific addresses and seller pickup points.
 * All operations are centered on the currently authenticated user ("me").
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // --- Buyer Saved Addresses ---

    /**
     * Saves a new delivery address for the current buyer.
     */
    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> addAddress(@RequestBody AddressDTO dto) {
        return ResponseEntity.ok(addressService.saveAddress(dto));
    }

    /**
     * Retrieves all saved addresses for the current buyer.
     */
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getMyAddresses() {
        return ResponseEntity.ok(addressService.getMyAddresses());
    }

    // --- Seller Pickup Points ---

    /**
     * Adds a new pickup point for the current seller.
     */
    @PostMapping("/seller-points")
    public ResponseEntity<SellerPointDTO> addSellerPoint(@RequestBody SellerPointDTO dto) {
        return ResponseEntity.ok(addressService.addSellerPoint(dto));
    }

    /**
     * Retrieves all pickup points managed by the current seller.
     */
    @GetMapping("/seller-points")
    public ResponseEntity<List<SellerPointDTO>> getMySellerPoints() {
        return ResponseEntity.ok(addressService.getMySellerPoints());
    }
}