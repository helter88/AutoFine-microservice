package com.autofine.notification_service.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MandateCreatedEvent(
        UUID mandateId,
        UUID userExternalId,
        BigDecimal fineAmount,
        LocalDateTime issueDate,
        UUID vehicleExternalId,
        int points,
        boolean pointsLimitExceeded
) {
}
