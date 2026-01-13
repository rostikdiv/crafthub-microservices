package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.address.AddressDTO;
import com.crafthub.user_service.dto.address.SellerPointDTO;
import com.crafthub.user_service.entity.SavedAddress;
import com.crafthub.user_service.entity.SellerPoint;
import com.crafthub.user_service.entity.User;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.repository.SavedAddressRepository;
import com.crafthub.user_service.repository.SellerPointRepository;
import com.crafthub.user_service.repository.UserRepository; // Якщо потрібно
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final SavedAddressRepository addressRepository;
    private final SellerPointRepository sellerPointRepository;

    // ✅ Метод, який дістає Юзера з токена (з контексту безпеки)
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized: User not found in context");
        }
        // Оскільки у нас User імплементує UserDetails, ми можемо його скастити
        return (User) authentication.getPrincipal();
    }

    // --- Методи для ПОКУПЦЯ (Мої адреси) ---

    @Transactional
    public AddressDTO saveAddress(AddressDTO dto) {
        User user = getCurrentUser(); // Автоматично беремо поточного юзера

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

    public List<AddressDTO> getMyAddresses() {
        User user = getCurrentUser();
        return addressRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToDTO).toList();
    }

    // --- Методи для ПРОДАВЦЯ (Мої точки видачі) ---

    @Transactional
    public SellerPointDTO addSellerPoint(SellerPointDTO dto) {
        User user = getCurrentUser();

        SellerProfile sellerProfile = user.getSellerProfile();
        if (sellerProfile == null) {
            throw new RuntimeException("User is not a seller");
        }

        SellerPoint point = SellerPoint.builder()
                .sellerProfile(sellerProfile)
                .name(dto.name())
                // ✅ Мапимо нові поля
                .cityRef(dto.cityRef())
                .cityName(dto.cityName())
                .region(dto.region())
                .streetName(dto.streetName())
                .building(dto.building())
                .apartment(dto.apartment())
                .zipCode(dto.zipCode())
                // Контакти
                .phone(dto.phone())
                .instructions(dto.instructions())
                .build();

        point = sellerPointRepository.save(point);
        return mapToSellerPointDTO(point);
    }

    @Transactional(readOnly = true)
    public List<SellerPointDTO> getMySellerPoints() {
        User user = getCurrentUser();

        // Якщо у користувача немає профілю продавця, повертаємо порожній список
        if (user.getSellerProfile() == null) {
            return List.of();
        }

        return sellerPointRepository.findAllBySellerProfileId(user.getSellerProfile().getId())
                .stream()
                .map(this::mapToSellerPointDTO)
                .toList();
    }


    // Мапери
    private AddressDTO mapToDTO(SavedAddress a) {
        return new AddressDTO(
                a.getId(), a.getTitle(), a.getProvider(), a.getDeliveryType(),
                a.getCityRef(), a.getCityName(), a.getRegion(),
                a.getBranchRef(), a.getBranchName(),
                a.getStreetName(), a.getBuilding(), a.getApartment(), a.getZipCode()
        );
    }

    private SellerPointDTO mapToSellerPointDTO(SellerPoint p) {
        return new SellerPointDTO(
                p.getId(),
                p.getName(),
                p.getCityRef(), p.getCityName(), p.getRegion(),
                p.getStreetName(), p.getBuilding(), p.getApartment(), p.getZipCode(),
                p.getPhone(),
                p.getInstructions()
        );
    }
}