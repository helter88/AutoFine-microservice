package com.autofine.notification_service.kafka;

import com.autofine.notification_service.model.dto.DrivingLicenseReinstatedDto;
import com.autofine.notification_service.model.dto.DrivingLicenseSuspendedDto;
import com.autofine.notification_service.model.dto.MandateCreatedEvent;
import com.autofine.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationKafkaConsumer {
    private final NotificationService notificationService;
    private static final Logger logger = LoggerFactory.getLogger(NotificationKafkaConsumer.class);

    public NotificationKafkaConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "mandate.created", groupId = "notification-data-group", containerFactory = "batchKafkaMandateCreatedContainerFactory")
    public void handleMandateCreatedBatch(List<MandateCreatedEvent> messages) {
        logger.info("Received a batch of {} messages from mandate.created", messages.size());
        messages.parallelStream().forEach(notificationService::processMandateCreated);
    }

    @KafkaListener(topics = "driving_license.suspended" , groupId = "notification-data-group", containerFactory = "batchKafkaLicenseSuspendedContainerFactory")
    public void handleLicenseSuspendedBatch(List<DrivingLicenseSuspendedDto> messages) {
        messages.parallelStream().forEach(notificationService::processLicenseSuspended);
    }

    @KafkaListener(topics = "driving_license.reinstated", groupId = "notification-data-group", containerFactory = "batchKafkaLicenseReinstateContainerFactory")
    public void handleLicenseReinstatedBatch(List<DrivingLicenseReinstatedDto> messages) {
        messages.parallelStream().forEach(notificationService::processLicenseReinstated);
    }
}
