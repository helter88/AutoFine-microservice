package com.autofine.notification_service.model.entity;

import com.autofine.notification_service.model.enums.NotificationStatus;
import com.autofine.notification_service.model.enums.NotificationType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class NotificationHistory {
    @Id
    private UUID id = UUID.randomUUID();

    private UUID userId;

    private UUID mandateId;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;
    private String contentMessage;

    private String errorMessage;

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

    public UUID getMandateId() {
        return mandateId;
    }

    public void setMandateId(UUID mandateId) {
        this.mandateId = mandateId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public String getContentMessage() {
        return contentMessage;
    }

    public void setContentMessage(String contentMessage) {
        this.contentMessage = contentMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
