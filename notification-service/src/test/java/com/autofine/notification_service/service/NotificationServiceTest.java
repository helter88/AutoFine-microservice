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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private NotificationHistoryRepository historyRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();
    private final UUID mandateId = UUID.randomUUID();
    private final String userEmail = "test@example.com";

    @Test
    void processMandateCreated_Success() {
        // Given
        MandateCreatedEvent event = new MandateCreatedEvent(
                mandateId,
                userId,
                BigDecimal.valueOf(100.00),
                LocalDateTime.now(),
                UUID.randomUUID(),
                5,
                false
        );

        UserNotification userNotification = new UserNotification(userId, userEmail);
        when(userNotificationRepository.findByUserId(userId)).thenReturn(Optional.of(userNotification));

        // When
        notificationService.processMandateCreated(event);

        // Then
        verify(userNotificationRepository).findByUserId(userId);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());

        ArgumentCaptor<NotificationHistory> historyCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        SimpleMailMessage sentMail = mailCaptor.getValue();
        NotificationHistory history = historyCaptor.getValue();

        assertAll(
                () -> assertEquals(userEmail, sentMail.getTo()[0]),
                () -> assertEquals("New traffic fine issued", sentMail.getSubject()),
                () -> assertTrue(sentMail.getText().contains("100,00 PLN")),
                () -> assertEquals(NotificationStatus.SENT, history.getStatus()),
                () -> assertEquals(NotificationType.MANDATE_CREATED, history.getNotificationType()),
                () -> assertEquals(mandateId, history.getMandateId())
        );
    }

    @Test
    void processMandateCreated_EmailFailure() {
        // Given
        UserNotification userNotification = new UserNotification(userId, userEmail);
        when(userNotificationRepository.findByUserId(userId)).thenReturn(Optional.of(userNotification));
        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // When
        notificationService.processMandateCreated(
                new MandateCreatedEvent(mandateId, userId, BigDecimal.TEN, LocalDateTime.now(), UUID.randomUUID(), 0, false)
        );

        // Then
        ArgumentCaptor<NotificationHistory> captor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository).save(captor.capture());

        NotificationHistory history = captor.getValue();
        assertAll(
                () -> assertEquals(NotificationStatus.FAILED, history.getStatus()),
                () -> assertEquals("SMTP error", history.getErrorMessage())
        );
    }
    @Test
    void processLicenseSuspended_Success() {
        // Given
        LocalDateTime suspensionDate = LocalDateTime.of(2024, 1, 1, 12, 30);
        DrivingLicenseSuspendedDto event = new DrivingLicenseSuspendedDto(userId, suspensionDate, true);
        UserNotification userNotification = new UserNotification(userId, userEmail);
        when(userNotificationRepository.findByUserId(userId)).thenReturn(Optional.of(userNotification));

        // When
        notificationService.processLicenseSuspended(event);

        // Then
        ArgumentCaptor<NotificationHistory> historyCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        NotificationHistory history = historyCaptor.getValue();
        String expectedDate = suspensionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        assertAll(
                () -> assertEquals(NotificationType.LICENSE_SUSPENDED, history.getNotificationType()),
                () -> assertTrue(history.getContentMessage().contains(expectedDate)),
                () -> assertNull(history.getMandateId())
        );
    }

    @Test
    void processLicenseReinstated_Success() {
        // Given
        LocalDateTime reinstatementDate = LocalDateTime.of(2024, 2, 1, 9, 0);
        DrivingLicenseReinstatedDto event = new DrivingLicenseReinstatedDto(userId, reinstatementDate, true);
        UserNotification userNotification = new UserNotification(userId, userEmail);
        when(userNotificationRepository.findByUserId(userId)).thenReturn(Optional.of(userNotification));

        // When
        notificationService.processLicenseReinstated(event);

        // Then
        ArgumentCaptor<NotificationHistory> historyCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        verify(historyRepository).save(historyCaptor.capture());

        NotificationHistory history = historyCaptor.getValue();
        String expectedDate = reinstatementDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        assertAll(
                () -> assertEquals(NotificationType.LICENSE_REINSTATED, history.getNotificationType()),
                () -> assertTrue(history.getContentMessage().contains(expectedDate)),
                () -> assertNull(history.getMandateId())
        );
    }
}