package com.autofine.mandate_service.model.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MandateCreatedDto(
        UUID mandateId,
        UUID userExternalId,
        BigDecimal fineAmount,
        java.time.LocalDateTime issueDate,
        UUID vehicleExternalId,
        Integer points,
        boolean pointsLimitExceeded
) {
}
