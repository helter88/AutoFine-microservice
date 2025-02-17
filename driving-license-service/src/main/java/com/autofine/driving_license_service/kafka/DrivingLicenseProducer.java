package com.autofine.driving_license_service.kafka;

import com.autofine.driving_license_service.model.dto.DrivingLicenseReinstatedDto;
import com.autofine.driving_license_service.model.dto.DrivingLicenseSuspendedDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DrivingLicenseProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public DrivingLicenseProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendLicenseSuspendedEvent(UUID userId) throws JsonProcessingException {
        DrivingLicenseSuspendedDto event = new DrivingLicenseSuspendedDto(
                userId,
                LocalDateTime.now(),
                true
        );
        kafkaTemplate.send("driving_license.suspended", objectMapper.writeValueAsString(event));
    }

    public void sendLicenseReinstatedEvent(UUID userId) throws JsonProcessingException {
        DrivingLicenseReinstatedDto event = new DrivingLicenseReinstatedDto(
                userId,
                LocalDateTime.now(),
                true
        );
        kafkaTemplate.send("driving_license.reinstated", objectMapper.writeValueAsString(event));
    }
}
