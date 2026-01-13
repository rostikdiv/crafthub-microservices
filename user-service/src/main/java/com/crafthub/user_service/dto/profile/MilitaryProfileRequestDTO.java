package com.crafthub.user_service.dto.profile;

public record MilitaryProfileRequestDTO(
        String unitNumber,     // "А1234"
        String edrpou,         // Код частини
        String commanderName,  // ПІБ командира
        String officialAddress // Адреса ППД
) {}