package com.autofine.driving_license_service.model.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DrivingLicenseSuspendedDto(
        UUID userExternalId,
        LocalDateTime suspensionDate,
        boolean isSuspended
) {
}
