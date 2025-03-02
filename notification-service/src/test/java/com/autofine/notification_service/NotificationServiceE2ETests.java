package com.autofine.notification_service;

import com.autofine.notification_service.config.KafkaTestConfig;
import com.autofine.notification_service.model.dto.DrivingLicenseReinstatedDto;
import com.autofine.notification_service.model.dto.DrivingLicenseSuspendedDto;
import com.autofine.notification_service.model.dto.MandateCreatedEvent;
import com.autofine.notification_service.model.entity.NotificationHistory;
import com.autofine.notification_service.model.entity.UserNotification;
import com.autofine.notification_service.model.enums.NotificationStatus;
import com.autofine.notification_service.model.enums.NotificationType;
import com.autofine.notification_service.repository.NotificationHistoryRepository;
import com.autofine.notification_service.repository.UserNotificationRepository;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@TestPropertySource(
        properties = {
                "spring.kafka.consumer.auto-offset-reset=earliest",
        }
)
@Import(KafkaTestConfig.class)
public class NotificationServiceE2ETests {

    private static final int SMTP_PORT = 3025;
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        // Pełna konfiguracja SMTP
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> String.valueOf(SMTP_PORT));
        registry.add("spring.mail.username", () -> "test@localhost");
        registry.add("spring.mail.password", () -> "test");
        registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
        registry.add("spring.mail.properties.mail.transport.protocol", () -> "smtp");

        // Ważne - całkowite wyłączenie uwierzytelniania
        registry.add("spring.mail.properties.mail.smtp.ssl.enable", () -> "false");
        registry.add("spring.mail.properties.mail.smtp.ssl.trust", () -> "*");
    }
    @Autowired
    private NotificationHistoryRepository historyRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    private GreenMail greenMail;

    @BeforeEach
    void setUp() {
        ServerSetup setup = new ServerSetup(SMTP_PORT, "localhost", ServerSetup.PROTOCOL_SMTP);
        setup.setServerStartupTimeout(10000);
        greenMail = new GreenMail(setup);
        greenMail.setUser("test@localhost", "test");
        greenMail.start();
        userNotificationRepository.deleteAll();
        historyRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        greenMail.stop();
    }

    @Test
    void shouldProcessMandateCreatedEvent() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserNotification userNotification = new UserNotification(userId, "test@example.com");
        userNotificationRepository.save(userNotification);

        MandateCreatedEvent event = new MandateCreatedEvent(
                UUID.randomUUID(),
                userId,
                new BigDecimal("150.50"),
                LocalDateTime.now(),
                UUID.randomUUID(),
                5,
                false
        );

        // When
        kafkaTemplate.send("mandate.created", event).get();

        // Then
        await().untilAsserted(() -> {
            List<NotificationHistory> histories = historyRepository.findAll();
            assertThat(histories).isNotEmpty();
            NotificationHistory history = histories.get(0);
            assertThat(history.getNotificationType()).isEqualTo(NotificationType.MANDATE_CREATED);
            assertThat(history.getStatus()).isEqualTo(NotificationStatus.SENT);
        });

        // Weryfikacja, czy mail faktycznie dotarł do GreenMail
        assertTrue(greenMail.waitForIncomingEmail(10000, 1));

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages[0].getSubject()).isEqualTo("New traffic fine issued");
        assertThat(receivedMessages[0].getContent().toString())
                .contains("150,50 PLN")
                .contains("Points: 5");
    }

    @Test
    void shouldProcessLicenseSuspendedEvent() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserNotification userNotification = new UserNotification(userId, "test@example.com");
        userNotificationRepository.save(userNotification);

        DrivingLicenseSuspendedDto event = new DrivingLicenseSuspendedDto(
                userId,
                LocalDateTime.now(),
                true
        );

        // When
        kafkaTemplate.send("driving_license.suspended", event).get();

        // Then
        await().untilAsserted(() -> {
            List<NotificationHistory> histories = historyRepository.findAll();
            assertThat(histories).isNotEmpty();

            NotificationHistory history = histories.get(0);
            assertThat(history.getNotificationType()).isEqualTo(NotificationType.LICENSE_SUSPENDED);
            assertThat(history.getStatus()).isEqualTo(NotificationStatus.SENT);
        });

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages[0].getSubject()).isEqualTo("Driving License Suspension Notification");
    }

    @Test
    void shouldProcessLicenseReinstatedEvent() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UserNotification userNotification = new UserNotification(userId, "test@example.com");
        userNotificationRepository.save(userNotification);

        DrivingLicenseReinstatedDto event = new DrivingLicenseReinstatedDto(
                userId,
                LocalDateTime.now(),
                true
        );

        // When
        kafkaTemplate.send("driving_license.reinstated", event).get();

        // Then
        await().untilAsserted(() -> {
            List<NotificationHistory> histories = historyRepository.findAll();
            assertThat(histories).isNotEmpty();

            NotificationHistory history = histories.get(0);
            assertThat(history.getNotificationType()).isEqualTo(NotificationType.LICENSE_REINSTATED);
            assertThat(history.getStatus()).isEqualTo(NotificationStatus.SENT);
        });

        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages[0].getSubject()).isEqualTo("Driving License Reinstatement Notification");
    }
    @Test
    void performanceTestMandateCreated() throws Exception {
        int messageCount = 100;
        // Given
        UUID userId = UUID.randomUUID();
        UserNotification userNotification = new UserNotification(userId, "perf-test@example.com");
        userNotificationRepository.save(userNotification);

        List<MandateCreatedEvent> events = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            events.add(new MandateCreatedEvent(
                    UUID.randomUUID(),
                    userId,
                    new BigDecimal("150.50"),
                    LocalDateTime.now(),
                    UUID.randomUUID(),
                    5,
                    false
            ));
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When - wysyłanie wszystkich wiadomości
        CompletableFuture<?>[] futures = events.stream()
                .map(event -> kafkaTemplate.send("mandate.created", event))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).get();

        await().until(() -> historyRepository.count() == messageCount);

        stopWatch.stop();

        // Then
        System.out.printf("[MANDATE_CREATED]Processed %d messages in %d ms%n",
                messageCount, stopWatch.getTotalTimeMillis());

    }

    @Test
    void performanceTestLicenseSuspended() throws Exception {
        int messageCount = 100;
        // Given
        UUID userId = UUID.randomUUID();
        UserNotification userNotification = new UserNotification(userId, "perf-test@example.com");
        userNotificationRepository.save(userNotification);

        List<DrivingLicenseSuspendedDto> events = IntStream.range(0, messageCount)
                .mapToObj(i -> new DrivingLicenseSuspendedDto(
                        userId,
                        LocalDateTime.now().plusMinutes(i), // unikalne daty dla każdego eventu
                        true
                ))
                .toList();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        CompletableFuture<?>[] futures = events.stream()
                .map(event -> kafkaTemplate.send("driving_license.suspended", event))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).get();

        await().until(() -> historyRepository.count() == messageCount);

        stopWatch.stop();

        // Then
        System.out.printf("[LICENSE_SUSPENDED] Przetworzono %d wiadomości w %d ms%n",
                messageCount, stopWatch.getTotalTimeMillis());
    }

    @Test
    void performanceTestLicenseReinstated() throws Exception {
        int messageCount = 100;
        // Given
        UUID userId = UUID.randomUUID();
        UserNotification userNotification = new UserNotification(userId, "perf-test@example.com");
        userNotificationRepository.save(userNotification);

        List<DrivingLicenseReinstatedDto> events = IntStream.range(0, messageCount)
                .mapToObj(i -> new DrivingLicenseReinstatedDto(
                        userId,
                        LocalDateTime.now().plusMinutes(i), // unikalne daty
                        true
                ))
                .toList();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // When
        CompletableFuture<?>[] futures = events.stream()
                .map(event -> kafkaTemplate.send("driving_license.reinstated", event))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).get();

        await().until(() -> historyRepository.count() == messageCount);

        stopWatch.stop();

        // Then
        System.out.printf("[LICENSE_REINSTATED] Przetworzono %d wiadomości w %d ms%n",
                messageCount, stopWatch.getTotalTimeMillis());
    }
}
