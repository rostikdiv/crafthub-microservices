package com.crafthub.delivery_service.dto.request;

import com.crafthub.delivery_service.entity.DeliveryProvider;
import java.util.List;

public record LocationCreateDTO(
        DeliveryProvider provider, // NOVA_POSHTA
        String externalId,         // "ref-lviv-np"
        String nameUkr,            // "Львів"
        String region,             // "Львівська область"
        List<BranchCreateDTO> branches // Вкладений список відділень
) {}