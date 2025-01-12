package com.autofine.fotoradar_data_service.dto;

import java.time.LocalDateTime;

public record FotoradarDataReceivedDto(
        String radarExternalId,
        LocalDateTime eventTimestamp,
        Integer speedLimit,
        Integer vehicleSpeed,
        String licensePlate,
        String imageUrl
) {
}
