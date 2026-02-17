package com.crafthub.user_service.service;

import com.crafthub.user_service.dto.address.SellerPointDTO;
import com.crafthub.user_service.entity.SellerPoint;
import com.crafthub.user_service.entity.SellerProfile;
import com.crafthub.user_service.exception.ResourceNotFoundException;
import com.crafthub.user_service.exception.BusinessException;
import com.crafthub.user_service.repository.SellerPointRepository;
import com.crafthub.user_service.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerPointService {

    private final SellerPointRepository pointRepository;
    private final SellerProfileRepository profileRepository;

    @Transactional
    public SellerPointDTO createPoint(UUID userId, SellerPointDTO dto) {
        SellerProfile profile = getProfileByUserId(userId);

        SellerPoint point = new SellerPoint();
        point.setSellerProfile(profile);
        updatePointFromDTO(point, dto);

        pointRepository.save(point);
        return mapToDTO(point);
    }

    @Transactional
    public SellerPointDTO updatePoint(UUID userId, UUID pointId, SellerPointDTO dto) {
        SellerProfile profile = getProfileByUserId(userId);

        SellerPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup point not found"));

        if (!point.getSellerProfile().getId().equals(profile.getId())) {
            throw new BusinessException("Access denied: You do not own this pickup point");
        }

        updatePointFromDTO(point, dto);
        pointRepository.save(point);
        return mapToDTO(point);
    }

    @Transactional
    public void deletePoint(UUID userId, UUID pointId) {
        SellerProfile profile = getProfileByUserId(userId);

        SellerPoint point = pointRepository.findById(pointId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup point not found"));

        if (!point.getSellerProfile().getId().equals(profile.getId())) {
            throw new BusinessException("Access denied: You do not own this pickup point");
        }

        pointRepository.delete(point);
    }

    @Transactional(readOnly = true)
    public List<SellerPointDTO> getMyPoints(UUID userId) {
        SellerProfile profile = getProfileByUserId(userId);
        return pointRepository.findAllBySellerProfileId(profile.getId()).stream()
                .map(this::mapToDTO)
                .toList();
    }

    private SellerProfile getProfileByUserId(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for user: " + userId));
    }

    private void updatePointFromDTO(SellerPoint point, SellerPointDTO dto) {
        point.setName(dto.name());
        point.setCityRef(dto.cityRef());
        point.setCityName(dto.cityName());
        point.setRegion(dto.region());
        point.setStreetName(dto.streetName());
        point.setBuilding(dto.building());
        point.setApartment(dto.apartment());
        point.setZipCode(dto.zipCode());
        point.setPhone(dto.phone());
        point.setInstructions(dto.instructions());
    }

    private SellerPointDTO mapToDTO(SellerPoint point) {
        return new SellerPointDTO(
                point.getId(),
                point.getName(),
                point.getCityRef(),
                point.getCityName(),
                point.getRegion(),
                point.getStreetName(),
                point.getBuilding(),
                point.getApartment(),
                point.getZipCode(),
                point.getPhone(),
                point.getInstructions());
    }
}
