package com.autofine.fotoradar_data_provider;

import java.time.LocalDateTime;

public record FotoradarDataProvidedDto(
        String radarId,

        LocalDateTime eventTimestamp,

        int vehicleSpeed,

        String licensePlate,

        String imageUrl,

        int speedLimit
) {
}
