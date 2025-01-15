package com.autofine.fotoradar_data_service.service;

import com.autofine.fotoradar_data_service.model.RadarData;
import com.autofine.fotoradar_data_service.dto.FotoradarDataProvidedDto;
import com.autofine.fotoradar_data_service.dto.FotoradarDataReceivedDto;
import com.autofine.fotoradar_data_service.repository.RadarDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FotoradarDataService {
    private static final Logger logger = LoggerFactory.getLogger(FotoradarDataService.class);
    private final RadarDataRepository radarDataRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC_RECEIVED = "fotoradar.data.received";

    public FotoradarDataService(RadarDataRepository radarDataRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.radarDataRepository = radarDataRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    // TODO bardziej dziel na podmetody
    @KafkaListener(topics = "fotoradar.data.provided", groupId = "fotoradar-data-group", containerFactory = "batchKafkaListenerContainerFactory")
    public void receiveFotoradarDataBatch(List<FotoradarDataProvidedDto> messages) {
        logger.info("Received a batch of {} messages", messages.size());
        // TODO zrównoleglajmy :)
        List<RadarData> validatedData = messages.stream()
                .map(this::processFotoradarData)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (!validatedData.isEmpty()) {
            radarDataRepository.saveAll(validatedData);

            validatedData.forEach(data -> {
                FotoradarDataReceivedDto receivedDto = new FotoradarDataReceivedDto(
                        data.getRadarExternalId(),
                        data.getEventTimestamp(),
                        data.getSpeedLimit(),
                        data.getVehicleSpeed(),
                        data.getLicensePlate(),
                        data.getImageUrl()
                );
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                try {
                    String jsonData = objectMapper.writeValueAsString(receivedDto);
                    kafkaTemplate.send(TOPIC_RECEIVED, jsonData);
                    logger.info("Wysłano wiadomość do tematu {}: {}", TOPIC_RECEIVED, jsonData);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            });
        }
    }

    private RadarData processFotoradarData(FotoradarDataProvidedDto providedDto) {
        try {

            // TODO rzuć okiem na jakarta validation
            if (providedDto.radarId() == null || providedDto.radarId().isEmpty() ||
                    providedDto.eventTimestamp() == null ||
                    providedDto.licensePlate() == null || providedDto.licensePlate().isEmpty()) {
                // TODO minimum to jakiś komunikat w logach (uwzględnić anonimizację) / slf4j
                return null;
            }

            return new RadarData(
                    providedDto.radarId(),
                    providedDto.eventTimestamp(),
                    providedDto.speedLimit(),
                    providedDto.vehicleSpeed(),
                    providedDto.licensePlate(),
                    providedDto.imageUrl()
            );

        } catch (Exception e) {
            // TODO minimum to jakiś komunikat w logach (uwzględnić anonimizację) / slf4j
            return null;
        }
    }
}
