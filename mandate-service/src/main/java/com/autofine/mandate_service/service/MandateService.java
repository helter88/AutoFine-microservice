package com.autofine.mandate_service.service;


import com.autofine.mandate_service.model.dto.FotoradarDataReceivedDto;
import com.autofine.mandate_service.model.dto.MandateCreatedDto;
import com.autofine.mandate_service.model.entity.Mandate;
import com.autofine.mandate_service.model.enums.PenaltyRate;
import com.autofine.mandate_service.model.enums.PointRate;
import com.autofine.mandate_service.repository.MandateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

@Service
public class MandateService {
    private static final Logger logger = LoggerFactory.getLogger(MandateService.class);
    private final MandateRepository mandateRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String MANDATE = "fotoradar.data.received";

    private static final int POINTS_LIMIT_BEFORE_LICENSE_SUSPENSION = 24;
    private static final String MANDATE_CREATED_TOPIC = "mandate.created";

    private final VehicleOwnerService vehicleOwnerService;
    private final ObjectMapper objectMapper;

    public MandateService(MandateRepository mandateRepository, KafkaTemplate<String, String> kafkaTemplate, VehicleOwnerService vehicleOwnerService) {
        this.mandateRepository = mandateRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.vehicleOwnerService = vehicleOwnerService;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    @Transactional
    @Async("mandateDataExecutor")
    public void processFotoradarData(FotoradarDataReceivedDto data) throws JsonProcessingException {

            int speedExcess = data.vehicleSpeed() - data.speedLimit();
            if (speedExcess < 0) {
                logger.info("No speed excess detected for radar data: {}", data.radarExternalId());
                return;
            }

            VehicleOwnerService.VehicleOwnerInfo ownerInfo = vehicleOwnerService.getVehicleOwnerInfo(data.licensePlate());
            if (ownerInfo == null) {
                logger.error("User not found for license plate: {}", data.licensePlate());
                return;
            }

            BigDecimal fineAmount = PenaltyRate.calculateFineAmount(speedExcess);
            int points = PointRate.calculatePoints(speedExcess);

            Mandate mandate = createMandate(data, fineAmount, points, ownerInfo.ownerId(), ownerInfo.vehicleId());

            boolean pointsLimitExceeded = ownerInfo.pointsNumber() + points >= POINTS_LIMIT_BEFORE_LICENSE_SUSPENSION;

            publishMandateCreatedEvent(mandate, ownerInfo.ownerId(), ownerInfo.vehicleId(), pointsLimitExceeded);
    }

    private Mandate createMandate(FotoradarDataReceivedDto data, BigDecimal fineAmount, int points, UUID ownerId, UUID vehicleId) {
        Mandate mandate = new Mandate();
        mandate.setUserExternalId(ownerId);
        mandate.setVehicleExternalId(vehicleId);
        mandate.setRadarDataId(data.radarExternalId());
        mandate.setViolationTimestamp(data.eventTimestamp());
        mandate.setFineAmount(fineAmount);
        mandate.setPoints(points);
        mandate.setStatus("NEW");

        return mandateRepository.save(mandate);
    }

    private void publishMandateCreatedEvent(Mandate mandate, UUID ownerId, UUID vehicleId, boolean pointsLimitExceeded) throws JsonProcessingException {
        MandateCreatedDto mandateCreated = new MandateCreatedDto(
                mandate.getId(),
                ownerId,
                mandate.getFineAmount(),
                mandate.getCreatedAt(),
                vehicleId,
                mandate.getPoints(),
                pointsLimitExceeded
        );
        kafkaTemplate.send(MANDATE_CREATED_TOPIC, serializeMandateCreatedDtoToJSON(mandateCreated));
    }

    private String serializeMandateCreatedDtoToJSON(MandateCreatedDto mandateCreated) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mandateCreated);
    }
}
