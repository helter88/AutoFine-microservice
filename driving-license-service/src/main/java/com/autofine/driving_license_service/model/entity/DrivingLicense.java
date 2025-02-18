package com.autofine.driving_license_service.model.entity;

import com.autofine.driving_license_service.model.enums.LicenseStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class DrivingLicense {
    @Id
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private LicenseStatus status = LicenseStatus.VALID;

    private int points = 0;

    private LocalDate suspensionStartDate;
    private LocalDate suspensionEndDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public DrivingLicense() {
    }

    public DrivingLicense(UUID userId, LicenseStatus status, LocalDate suspensionStartDate, LocalDate suspensionEndDate) {
        this.userId = userId;
        this.status = status;
        this.suspensionStartDate = suspensionStartDate;
        this.suspensionEndDate = suspensionEndDate;
    }


    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LicenseStatus getStatus() {
        return status;
    }

    public void setStatus(LicenseStatus status) {
        this.status = status;
    }

    public LocalDate getSuspensionStartDate() {
        return suspensionStartDate;
    }

    public void setSuspensionStartDate(LocalDate suspensionStartDate) {
        this.suspensionStartDate = suspensionStartDate;
    }

    public LocalDate getSuspensionEndDate() {
        return suspensionEndDate;
    }

    public void setSuspensionEndDate(LocalDate suspensionEndDate) {
        this.suspensionEndDate = suspensionEndDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
