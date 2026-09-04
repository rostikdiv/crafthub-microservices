package com.milhub.user_service.service;

import com.milhub.user_service.dto.address.SellerPointDTO;
import com.milhub.user_service.entity.SellerPoint;
import com.milhub.user_service.entity.SellerProfile;
import com.milhub.user_service.entity.User;
import com.milhub.user_service.exception.BusinessException;
import com.milhub.user_service.exception.ResourceNotFoundException;
import com.milhub.user_service.repository.SellerPointRepository;
import com.milhub.user_service.repository.SellerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerPointServiceTest {

    @Mock
    private SellerPointRepository pointRepository;

    @Mock
    private SellerProfileRepository profileRepository;

    @InjectMocks
    private SellerPointService sellerPointService;

    private final UUID userId = UUID.randomUUID();
    private final UUID profileId = UUID.randomUUID();
    private final UUID pointId = UUID.randomUUID();
    private SellerProfile sellerProfile;
    private SellerPoint sellerPoint;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(userId).build();
        sellerProfile = SellerProfile.builder()
                .id(profileId)
                .user(user)
                .companyName("MilStore")
                .build();

        sellerPoint = SellerPoint.builder()
                .id(pointId)
                .sellerProfile(sellerProfile)
                .name("Point 1")
                .cityName("Kyiv")
                .region("Kyivska")
                .streetName("Khreshchatyk")
                .building("1")
                .phone("+380501234567")
                .build();
    }

    @Test
    void createPoint_WhenProfileFound_ShouldSaveAndReturnDTO() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));

        SellerPointDTO inputDto = new SellerPointDTO(
                null, "New Point", "ref-1", "Kyiv", "Kyivska",
                "Volodymyrska", "10", "5", "01001", "+380501112233", "Ring 5"
        );

        when(pointRepository.save(any(SellerPoint.class))).thenAnswer(invocation -> {
            SellerPoint p = invocation.getArgument(0);
            p.setId(pointId);
            return p;
        });

        SellerPointDTO result = sellerPointService.createPoint(userId, inputDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("New Point");
        assertThat(result.cityName()).isEqualTo("Kyiv");
        assertThat(result.phone()).isEqualTo("+380501112233");

        ArgumentCaptor<SellerPoint> captor = ArgumentCaptor.forClass(SellerPoint.class);
        verify(pointRepository).save(captor.capture());
        assertThat(captor.getValue().getSellerProfile()).isEqualTo(sellerProfile);
    }

    @Test
    void createPoint_WhenProfileNotFound_ShouldThrowException() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        SellerPointDTO inputDto = new SellerPointDTO(
                null, "New Point", null, "Kyiv", "Kyivska",
                "Street", "1", null, null, "+380501112233", null
        );

        assertThrows(ResourceNotFoundException.class, () -> sellerPointService.createPoint(userId, inputDto));
        verify(pointRepository, never()).save(any());
    }

    @Test
    void updatePoint_WhenValidAndOwner_ShouldUpdateAndReturnDTO() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findById(pointId)).thenReturn(Optional.of(sellerPoint));

        SellerPointDTO updateDto = new SellerPointDTO(
                pointId, "Updated Point", "ref-2", "Kyiv", "Kyivska",
                "Shevchenka", "20", null, "01002", "+380509998877", "New instruction"
        );

        SellerPointDTO result = sellerPointService.updatePoint(userId, pointId, updateDto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Updated Point");
        assertThat(result.streetName()).isEqualTo("Shevchenka");
        verify(pointRepository).save(sellerPoint);
    }

    @Test
    void updatePoint_WhenPointNotFound_ShouldThrowException() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findById(pointId)).thenReturn(Optional.empty());

        SellerPointDTO updateDto = new SellerPointDTO(
                pointId, "Updated Point", null, "Kyiv", "Kyivska",
                "Street", "1", null, null, "+380501112233", null
        );

        assertThrows(ResourceNotFoundException.class, () -> sellerPointService.updatePoint(userId, pointId, updateDto));
        verify(pointRepository, never()).save(any());
    }

    @Test
    void updatePoint_WhenNotOwner_ShouldThrowBusinessException() {
        SellerProfile otherProfile = SellerProfile.builder().id(UUID.randomUUID()).build();
        sellerPoint.setSellerProfile(otherProfile);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findById(pointId)).thenReturn(Optional.of(sellerPoint));

        SellerPointDTO updateDto = new SellerPointDTO(
                pointId, "Updated Point", null, "Kyiv", "Kyivska",
                "Street", "1", null, null, "+380501112233", null
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sellerPointService.updatePoint(userId, pointId, updateDto));
        assertThat(ex.getMessage()).contains("Access denied");
        verify(pointRepository, never()).save(any());
    }

    @Test
    void deletePoint_WhenValidAndOwner_ShouldDelete() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findById(pointId)).thenReturn(Optional.of(sellerPoint));

        sellerPointService.deletePoint(userId, pointId);

        verify(pointRepository).delete(sellerPoint);
    }

    @Test
    void deletePoint_WhenPointNotFound_ShouldThrowException() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findById(pointId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> sellerPointService.deletePoint(userId, pointId));
        verify(pointRepository, never()).delete(any());
    }

    @Test
    void deletePoint_WhenNotOwner_ShouldThrowBusinessException() {
        SellerProfile otherProfile = SellerProfile.builder().id(UUID.randomUUID()).build();
        sellerPoint.setSellerProfile(otherProfile);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findById(pointId)).thenReturn(Optional.of(sellerPoint));

        assertThrows(BusinessException.class, () -> sellerPointService.deletePoint(userId, pointId));
        verify(pointRepository, never()).delete(any());
    }

    @Test
    void getMyPoints_ShouldReturnListOfDtos() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(sellerProfile));
        when(pointRepository.findAllBySellerProfileId(profileId)).thenReturn(List.of(sellerPoint));

        List<SellerPointDTO> points = sellerPointService.getMyPoints(userId);

        assertThat(points).hasSize(1);
        assertThat(points.get(0).id()).isEqualTo(pointId);
        assertThat(points.get(0).name()).isEqualTo("Point 1");
    }
}
