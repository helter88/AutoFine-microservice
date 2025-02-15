package com.autofine.mandate_service;

import com.autofine.mandate_service.config.KafkaTestConfig;
import com.autofine.mandate_service.model.dto.FotoradarDataReceivedDto;
import com.autofine.mandate_service.model.dto.MandateCreatedDto;
import com.autofine.mandate_service.repository.MandateRepository;
import com.autofine.mandate_service.service.VehicleOwnerService;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@TestPropertySource(
        properties = {
                "spring.kafka.consumer.auto-offset-reset=earliest",
        }
)
@DirtiesContext
@Import(KafkaTestConfig.class)
public class MandateServiceE2ETest {
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
    private KafkaTemplate<String, FotoradarDataReceivedDto> fotoradarDataKafkaTemplate;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private MandateRepository mandateRepository;

    @MockitoBean
    private VehicleOwnerService vehicleOwnerService;

    private static final String INPUT_TOPIC = "fotoradar.data.received";

    private static final String OUTPUT_TOPIC = "mandate.created";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private MandateCreatedDto deserializeMandateCreatedDto(String json) {
        try {
            return objectMapper.readValue(json, MandateCreatedDto.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize MandateCreatedDto", e);
        }
    }

    @Test
    void shouldProcessSingleMessageAndProduceMandateEvent() throws Exception {
        // Given
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(vehicleOwnerService.getVehicleOwnerInfo(anyString()))
                .thenReturn(new VehicleOwnerService.VehicleOwnerInfo(ownerId, vehicleId, 20));

        FotoradarDataReceivedDto data = new FotoradarDataReceivedDto(
                UUID.randomUUID(),
                LocalDateTime.now(),
                50,
                70,
                "ABC123",
                "http://example.com/image.jpg"
        );

        // When
//        String jsonMessage = objectMapper.writeValueAsString(data);
        fotoradarDataKafkaTemplate.send(INPUT_TOPIC, data);

        // Then
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(mandateRepository.count()).isEqualTo(1);

                    Consumer<String, String> consumer = consumerFactory.createConsumer();
                    consumer.subscribe(Collections.singleton(OUTPUT_TOPIC));

                    ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(5000));
                    consumer.close();

                    assertThat(records).isNotNull();
                    assertThat(records.isEmpty()).isFalse();
                    assertThat(records.count()).isEqualTo(1);
                    ConsumerRecord<String, String> record = records.iterator().next(); // Pobierz pierwszy (i jedyny) rekord
                    String jsonOutputMessage = record.value(); // Pobierz wartość rekordu (JSON jako String)
                    MandateCreatedDto mandateCreatedDto = deserializeMandateCreatedDto(jsonOutputMessage);
                    assertThat(mandateCreatedDto.userExternalId()).isEqualTo(ownerId);
                    assertThat(mandateCreatedDto.points()).isEqualTo(3);
                    assertThat(mandateCreatedDto.fineAmount()).isEqualTo(BigDecimal.valueOf(300));
                    assertThat(mandateCreatedDto.pointsLimitExceeded()).isFalse();

                });
    }
    @Test
    void testParallelProcessingPerformance() throws Exception {
        // Given
        int messageCount = 100;
        when(vehicleOwnerService.getVehicleOwnerInfo(anyString()))
                .thenReturn(new VehicleOwnerService.VehicleOwnerInfo(UUID.randomUUID(), UUID.randomUUID(), 0));

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            FotoradarDataReceivedDto data = new FotoradarDataReceivedDto(
                    UUID.randomUUID(),
                    LocalDateTime.now(),
                    50,
                    70,
                    "ABC" + i,
                    "http://example.com/image.jpg"
            );
            // When
            fotoradarDataKafkaTemplate.send(INPUT_TOPIC, data);
        }

        // Then
        await().until(() -> mandateRepository.count() == messageCount);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Processing time for " + messageCount + " messages: " + duration + " ms");
    }
}
