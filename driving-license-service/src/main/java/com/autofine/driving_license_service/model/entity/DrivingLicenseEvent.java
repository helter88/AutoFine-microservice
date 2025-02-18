package com.autofine.driving_license_service.model.entity;

import com.autofine.driving_license_service.model.enums.LicenseEventType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class DrivingLicenseEvent {
    @Id
    private UUID id = UUID.randomUUID();

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private LicenseEventType eventType;

    private LocalDateTime eventDate;

    private String reason;

    public DrivingLicenseEvent() {
    }

    public DrivingLicenseEvent(UUID userId, LicenseEventType eventType, LocalDateTime eventDate, String reason) {
        this.userId = userId;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LicenseEventType getEventType() {
        return eventType;
    }

    public void setEventType(LicenseEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
