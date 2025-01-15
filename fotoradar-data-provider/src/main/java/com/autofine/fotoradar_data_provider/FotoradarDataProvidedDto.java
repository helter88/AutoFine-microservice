package com.autofine.fotoradar_data_provider;

import java.time.LocalDateTime;

public record FotoradarDataProvidedDto(
        String radarId,

        LocalDateTime eventTimestamp, // jeśli zakładamy, że dane spływają z jednej i tej samej strefy czasowej, to ok (1. są kraje mające wiele stref, 2. mogłyby przyjść dane z innego państwa) - chcesz przećwiczyć strefy?

        Unit speedUnit,

        int vehicleSpeed, // nie mieliśmy tu dawać jeszcze jednostki prędkości?

        String licensePlate,

        String imageUrl,

        int speedLimit
) {

  enum Unit {
    KMH,
    MPH
  }
}
