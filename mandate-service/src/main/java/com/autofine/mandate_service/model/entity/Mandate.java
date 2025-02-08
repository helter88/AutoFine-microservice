package com.autofine.mandate_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Mandate {

    @Id
    private UUID id = UUID.randomUUID();
    private UUID userExternalId;
    private UUID vehicleExternalId;
    private UUID radarDataId;
    private LocalDateTime violationTimestamp;
    private BigDecimal fineAmount;
    private Integer points;
    private String status;
    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;
}