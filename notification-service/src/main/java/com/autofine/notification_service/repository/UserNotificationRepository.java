package com.autofine.notification_service.repository;

import com.autofine.notification_service.model.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    Optional<UserNotification> findByUserId(UUID userId);
}
