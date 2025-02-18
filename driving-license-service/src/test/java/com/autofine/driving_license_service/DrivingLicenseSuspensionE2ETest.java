package com.autofine.driving_license_service;

import com.autofine.driving_license_service.config.KafkaTestConfig;
import com.autofine.driving_license_service.model.dto.DrivingLicenseSuspendedDto;
import com.autofine.driving_license_service.model.dto.MandateCreatedDto;
import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.enums.LicenseStatus;
import com.autofine.driving_license_service.repository.DrivingLicenseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@TestPropertySource(
        properties = {
                "spring.kafka.consumer.auto-offset-reset=earliest",
        }
)
@DirtiesContext
@Import(KafkaTestConfig.class)
public class DrivingLicenseSuspensionE2ETest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DrivingLicenseRepository drivingLicenseRepository;

    @Autowired
    private KafkaTemplate<String, MandateCreatedDto> mandateDataKafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    private static final String INPUT_TOPIC = "mandate.created";

    private static final String OUTPUT_TOPIC = "driving_license.suspended";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private DrivingLicenseSuspendedDto deserializeDrivingLicenseSuspendedDto(String json) {
        try {
            return objectMapper.readValue(json, DrivingLicenseSuspendedDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize DrivingLicenseSuspendedDto", e);
        }
    }

    @Test
    void whenMandateExceedsPoints_thenLicenseSuspendedAndEventProduced() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        drivingLicenseRepository.save(new DrivingLicense(userId, LicenseStatus.VALID, null, null));

        MandateCreatedDto mandate = new MandateCreatedDto(
                UUID.randomUUID(), userId, BigDecimal.valueOf(500), LocalDateTime.now(), UUID.randomUUID(), 15, true
        );

        // When
        mandateDataKafkaTemplate.send(INPUT_TOPIC, mandate).get();

        // Then
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    DrivingLicense license = drivingLicenseRepository.findById(userId).orElseThrow();
                    assertThat(license.getStatus()).isEqualTo(LicenseStatus.SUSPENDED);

                    Consumer<String, String> consumer = consumerFactory.createConsumer();
                    consumer.subscribe(Collections.singleton(OUTPUT_TOPIC));
                    ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(5000));
                    consumer.close();
                    assertThat(records).isNotNull();
                    assertThat(records.isEmpty()).isFalse();
                    assertThat(records.count()).isEqualTo(1);
                    ConsumerRecord<String, String> record = records.iterator().next();
                    String jsonOutputMessage = record.value();
                    DrivingLicenseSuspendedDto mandateCreatedDto = deserializeDrivingLicenseSuspendedDto(jsonOutputMessage);
                    assertThat(mandateCreatedDto.userExternalId()).isEqualTo(userId);
                    assertThat(mandateCreatedDto.isSuspended()).isEqualTo(true);

                });
    }
}
