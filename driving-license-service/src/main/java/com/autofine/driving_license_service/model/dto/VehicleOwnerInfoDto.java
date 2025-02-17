package com.autofine.driving_license_service.model.dto;

import java.util.UUID;

public record VehicleOwnerInfoDto(
        UUID userId,
        UUID vehicleId,
        int pointsNumber
) {
}
