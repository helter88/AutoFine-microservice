package com.autofine.fotoradar_data_service.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class RadarData {
    @Id
    private UUID id = UUID.randomUUID();
    private String radarExternalId;
    private LocalDateTime eventTimestamp;
    private int speedLimit;
    private int vehicleSpeed;
    private String licensePlate;
    private String imageUrl;

    @Column(updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    public RadarData(){}

    public RadarData(String radarExternalId, LocalDateTime eventTimestamp, Integer speedLimit, Integer vehicleSpeed, String licensePlate, String imageUrl) {
        this.radarExternalId = radarExternalId;
        this.eventTimestamp = eventTimestamp;
        this.speedLimit = speedLimit;
        this.vehicleSpeed = vehicleSpeed;
        this.licensePlate = licensePlate;
        this.imageUrl = imageUrl;
    }

    public UUID getId() {
        return id;
    }

    public String getRadarExternalId() {
        return radarExternalId;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public int getSpeedLimit() {
        return speedLimit;
    }

    public int getVehicleSpeed() {
        return vehicleSpeed;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
