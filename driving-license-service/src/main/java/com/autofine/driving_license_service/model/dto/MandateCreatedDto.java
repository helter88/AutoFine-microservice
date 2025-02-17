package com.autofine.driving_license_service.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MandateCreatedDto(
        UUID mandateId,
        UUID userExternalId,
        BigDecimal fineAmount,
        LocalDateTime issueDate,
        UUID vehicleExternalId,
        int points,
        boolean pointsLimitExceeded
) {
}
