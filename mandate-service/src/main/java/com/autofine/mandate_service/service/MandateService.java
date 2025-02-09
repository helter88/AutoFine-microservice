package com.autofine.mandate_service.service;


import com.autofine.mandate_service.model.dto.FotoradarDataReceivedDto;
import com.autofine.mandate_service.model.dto.MandateCreatedDto;
import com.autofine.mandate_service.model.entity.Mandate;
import com.autofine.mandate_service.model.enums.PenaltyRate;
import com.autofine.mandate_service.model.enums.PointRate;
import com.autofine.mandate_service.repository.MandateRepository;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String MANDATE = "fotoradar.data.received";

    private static final int POINTS_LIMIT_BEFORE_LICENSE_SUSPENSION = 24;
    private static final String MANDATE_CREATED_TOPIC = "mandate.created";

    private final VehicleOwnerService vehicleOwnerService;

    public MandateService(MandateRepository mandateRepository, KafkaTemplate<String, Object> kafkaTemplate, VehicleOwnerService vehicleOwnerService) {
        this.mandateRepository = mandateRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.vehicleOwnerService = vehicleOwnerService;
    }

    @Transactional
    @Async("MandateDataExecutor")
    public void processFotoradarData(FotoradarDataReceivedDto data) {

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

            BigDecimal fineAmount = calculateFineAmount(speedExcess);
            int points = calculatePoints(speedExcess);

            Mandate mandate = createMandate(data, fineAmount, points, ownerInfo.ownerId(), ownerInfo.vehicleId());

            boolean pointsLimitExceeded = ownerInfo.pointsNumber() + points >= POINTS_LIMIT_BEFORE_LICENSE_SUSPENSION;

            publishMandateCreatedEvent(mandate, ownerInfo.ownerId(), ownerInfo.vehicleId(), pointsLimitExceeded);
    }

    private BigDecimal calculateFineAmount(int speedExcess) {
        return Arrays.stream(PenaltyRate.values())
                .filter(rate -> rate.getMinSpeed() <= speedExcess && rate.getMaxSpeed() >= speedExcess)
                .findFirst()
                .map(PenaltyRate::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    private int calculatePoints(int speedExcess) {
        return Arrays.stream(PointRate.values())
                .filter(rate -> rate.getMinSpeed() <= speedExcess && rate.getMaxSpeed() >= speedExcess)
                .findFirst()
                .map(PointRate::getPoints)
                .orElse(0);
    }

    private Mandate createMandate(FotoradarDataReceivedDto data, BigDecimal fineAmount, int points, UUID ownerId, UUID vehicleId) {
        Mandate mandate = new Mandate();
        mandate.setUserExternalId(ownerId);
        mandate.setVehicleExternalId(vehicleId);
        mandate.setRadarDataId(data.radarExternalId());
        mandate.setViolationTimestamp(data.eventTimestamp());
        mandate.setFineAmount(fineAmount);
        mandate.setPoints(points);
        mandate.setStatus("NEW"); // Default status

        return mandateRepository.save(mandate);
    }

    private void publishMandateCreatedEvent(Mandate mandate, UUID ownerId, UUID vehicleId, boolean pointsLimitExceeded) {
        MandateCreatedDto mandateCreated = new MandateCreatedDto(
                mandate.getId(),
                ownerId,
                mandate.getFineAmount(),
                mandate.getCreatedAt(),
                vehicleId,
                mandate.getPoints(),
                pointsLimitExceeded
        );
        kafkaTemplate.send(MANDATE_CREATED_TOPIC, mandateCreated);
    }
}
