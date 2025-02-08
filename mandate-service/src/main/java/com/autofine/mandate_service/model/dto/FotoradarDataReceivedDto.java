package com.autofine.mandate_service.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record FotoradarDataReceivedDto(
        UUID radarExternalId,
        LocalDateTime eventTimestamp,
        Integer speedLimit,
        Integer vehicleSpeed,
        String licensePlate,
        String imageUrl
) {
}