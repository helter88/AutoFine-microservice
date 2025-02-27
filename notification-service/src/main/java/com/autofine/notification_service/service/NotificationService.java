package com.autofine.notification_service.service;

import com.autofine.notification_service.model.dto.DrivingLicenseReinstatedDto;
import com.autofine.notification_service.model.dto.DrivingLicenseSuspendedDto;
import com.autofine.notification_service.model.dto.MandateCreatedEvent;
import com.autofine.notification_service.model.entity.NotificationHistory;
import com.autofine.notification_service.model.entity.UserNotification;
import com.autofine.notification_service.model.enums.NotificationStatus;
import com.autofine.notification_service.model.enums.NotificationType;
import com.autofine.notification_service.repository.NotificationHistoryRepository;
import com.autofine.notification_service.repository.UserNotificationRepository;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class NotificationService {
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationHistoryRepository historyRepository;
    private final JavaMailSender mailSender;

    public NotificationService(UserNotificationRepository userNotificationRepository, NotificationHistoryRepository historyRepository, JavaMailSender mailSender) {
        this.userNotificationRepository = userNotificationRepository;
        this.historyRepository = historyRepository;
        this.mailSender = mailSender;
    }

    @Async
    public void processMandateCreated(MandateCreatedEvent event) {
        UserNotification userNotification = userNotificationRepository.findByUserId(event.userExternalId())
                .orElseThrow(() -> new RuntimeException("User notification data not found"));

        String subject = "New traffic fine issued";
        String content = String.format(
                "Dear User,\nA new fine has been issued in the amount of %s PLN.\nPoints: %s\nDate: %s",
                event.fineAmount(),
                event.points(),
                event.issueDate()
        );

        sendNotification(userNotification, subject, content, NotificationType.MANDATE_CREATED, event.mandateId());
    }


    private void sendNotification(UserNotification userNotification,
                                  String subject,
                                  String content,
                                  NotificationType type,
                                  UUID mandateId) {
        NotificationHistory history = new NotificationHistory();
        history.setUserId(userNotification.getUserId());
        history.setMandateId(mandateId);
        history.setNotificationType(type);
        history.setSentAt(LocalDateTime.now());
        history.setStatus(NotificationStatus.PENDING);
        history.setContentMessage(content);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userNotification.getEmail());
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);

            history.setStatus(NotificationStatus.SENT);
        } catch (MailException e) {
            history.setStatus(NotificationStatus.FAILED);
            history.setErrorMessage(e.getMessage());
        } finally {
            historyRepository.save(history);
        }
    }

    public void processLicenseSuspended(DrivingLicenseSuspendedDto event) {
        UserNotification userNotification = userNotificationRepository.findByUserId(event.userExternalId())
                .orElseThrow(() -> new RuntimeException("User notification data not found"));

        String subject = "Driving License Suspension Notification";
        String content = String.format(
                "Dear User,\n\nYour driving license has been suspended effective %s.\nReason: Exceeding the number of penalty points\n\n"
                        + "Please contact the relevant authorities for more information.",
                event.suspensionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );

        sendNotification(
                userNotification,
                subject,
                content,
                NotificationType.LICENSE_SUSPENDED,
                null
        );
    }

    public void processLicenseReinstated(DrivingLicenseReinstatedDto event) {
        UserNotification userNotification = userNotificationRepository.findByUserId(event.userExternalId())
                .orElseThrow(() -> new RuntimeException("User notification data not found"));

        String subject = "Driving License Reinstatement Notification";
        String content = String.format(
                "Dear User,\n\nYour driving license has been reinstated as of %s.\n\n"
                        + "You are now legally permitted to operate motor vehicles.",
                event.reinstatementDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );

        sendNotification(
                userNotification,
                subject,
                content,
                NotificationType.LICENSE_REINSTATED,
                null
        );
    }
}
