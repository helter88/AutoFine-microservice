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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserExternalId() {
        return userExternalId;
    }

    public void setUserExternalId(UUID userExternalId) {
        this.userExternalId = userExternalId;
    }

    public UUID getVehicleExternalId() {
        return vehicleExternalId;
    }

    public void setVehicleExternalId(UUID vehicleExternalId) {
        this.vehicleExternalId = vehicleExternalId;
    }

    public UUID getRadarDataId() {
        return radarDataId;
    }

    public void setRadarDataId(UUID radarDataId) {
        this.radarDataId = radarDataId;
    }

    public LocalDateTime getViolationTimestamp() {
        return violationTimestamp;
    }

    public void setViolationTimestamp(LocalDateTime violationTimestamp) {
        this.violationTimestamp = violationTimestamp;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}