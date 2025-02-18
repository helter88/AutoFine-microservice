package com.autofine.driving_license_service.kafka;

import com.autofine.driving_license_service.model.dto.MandateCreatedDto;
import com.autofine.driving_license_service.service.DrivingLicenseSuspensionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MandateCreatedConsumer {
    private final DrivingLicenseSuspensionService drivingLicenseSuspensionService;
    private static final Logger logger = LoggerFactory.getLogger(DrivingLicenseSuspensionService.class);

    public MandateCreatedConsumer(DrivingLicenseSuspensionService drivingLicenseSuspensionService) {
        this.drivingLicenseSuspensionService = drivingLicenseSuspensionService;
    }
    @KafkaListener(topics = "mandate.created", groupId = "driving-licence-data-group", containerFactory = "batchKafkaListenerContainerFactory")
    public void receiveMandateDataBatch(List<MandateCreatedDto> messages) {
        logger.info("Received a batch of {} messages", messages.size());
        messages.parallelStream().forEach(data -> {
            try {
                drivingLicenseSuspensionService.processMandateData(data);
            } catch (JsonProcessingException e) {
                logger.error("Error processing mandate data: {}", data, e);
            } catch (Exception e) {
                logger.error("Unexpected error processing mandate data: {}", data, e);
            }
        });
    }
}
