package com.autofine.fotoradar_data_service.dto;

import java.time.LocalDateTime;

public record FotoradarDataProvidedDto(
        String radarId,

        LocalDateTime eventTimestamp,

        String speedUnit,

        int vehicleSpeed,

        String licensePlate,

        String imageUrl,

        int speedLimit
) {

  enum Unit {
    KMH,
    MPH
  }
}
