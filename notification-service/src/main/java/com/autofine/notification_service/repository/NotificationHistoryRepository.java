package com.autofine.notification_service.repository;

import com.autofine.notification_service.model.entity.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, UUID> {
}
