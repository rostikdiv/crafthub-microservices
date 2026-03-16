package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.address.AddressDTO;
import com.crafthub.user_service.dto.address.SellerPointDTO;
import com.crafthub.user_service.entity.SavedAddress;
import com.crafthub.user_service.entity.SellerPoint;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.repository.SavedAddressRepository;
import com.crafthub.user_service.repository.SellerPointRepository;
import com.crafthub.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing user delivery addresses and seller pickup locations.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final SavedAddressRepository addressRepository;
    private final SellerPointRepository sellerPointRepository;
    private final UserRepository userRepository;

    /**
     * Helper to retrieve the current user from the SecurityContext.
     */
    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessException("Unauthorized: User not found in context");
        }

        String userIdStr = (String) auth.getPrincipal();
        return userRepository.findById(UUID.fromString(userIdStr))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // --- Buyer Address Operations ---

    /**
     * Saves a new delivery address for the buyer.
     */
    @Transactional
    public AddressDTO saveAddress(AddressDTO dto) {
        User user = getCurrentUser();

        SavedAddress address = SavedAddress.builder()
                .user(user)
                .title(dto.title())
                .provider(dto.provider())
                .deliveryType(dto.deliveryType())
                .cityRef(dto.cityRef()).cityName(dto.cityName()).region(dto.region())
                .branchRef(dto.branchRef()).branchName(dto.branchName())
                .streetName(dto.streetName()).building(dto.building())
                .apartment(dto.apartment()).zipCode(dto.zipCode())
                .build();

        address = addressRepository.save(address);
        return mapToDTO(address);
    }

    /**
     * Retrieves all saved addresses for the current buyer.
     */
    public List<AddressDTO> getMyAddresses() {
        User user = getCurrentUser();
        return addressRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToDTO).toList();
    }

    // --- Seller Point Operations ---

    /**
     * Adds a new pickup point for a seller profile.
     */
    @Transactional
    public SellerPointDTO addSellerPoint(SellerPointDTO dto) {
        User user = getCurrentUser();

        SellerProfile sellerProfile = user.getSellerProfile();
        if (sellerProfile == null) {
            throw new BusinessException("User is not a seller");
        }

        SellerPoint point = SellerPoint.builder()
                .sellerProfile(sellerProfile)
                .name(dto.name())
                .cityRef(dto.cityRef())
                .cityName(dto.cityName())
                .region(dto.region())
                .streetName(dto.streetName())
                .building(dto.building())
                .apartment(dto.apartment())
                .zipCode(dto.zipCode())
                .phone(dto.phone())
                .instructions(dto.instructions())
                .build();

        point = sellerPointRepository.save(point);
        return mapToSellerPointDTO(point);
    }

    /**
     * Retrieves all points belonging to the current seller.
     */
    @Transactional(readOnly = true)
    public List<SellerPointDTO> getMySellerPoints() {
        User user = getCurrentUser();

        if (user.getSellerProfile() == null) {
            return List.of();
        }

        return sellerPointRepository.findAllBySellerProfileId(user.getSellerProfile().getId())
                .stream()
                .map(this::mapToSellerPointDTO)
                .toList();
    }

    // --- Mappers ---

    private AddressDTO mapToDTO(SavedAddress a) {
        return new AddressDTO(
                a.getId(), a.getTitle(), a.getProvider(), a.getDeliveryType(),
                a.getCityRef(), a.getCityName(), a.getRegion(),
                a.getBranchRef(), a.getBranchName(),
                a.getStreetName(), a.getBuilding(), a.getApartment(), a.getZipCode());
    }

    private SellerPointDTO mapToSellerPointDTO(SellerPoint p) {
        return new SellerPointDTO(
                p.getId(),
                p.getName(),
                p.getCityRef(), p.getCityName(), p.getRegion(),
                p.getStreetName(), p.getBuilding(), p.getApartment(), p.getZipCode(),
                p.getPhone(),
                p.getInstructions());
    }
}