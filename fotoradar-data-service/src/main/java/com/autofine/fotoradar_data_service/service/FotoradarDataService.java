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


    @KafkaListener(topics = "fotoradar.data.provided", groupId = "fotoradar-data-group", containerFactory = "batchKafkaListenerContainerFactory")
    public void receiveFotoradarDataBatch(List<FotoradarDataProvidedDto> messages) {
        logger.info("Received a batch of {} messages", messages.size());
        List<RadarData> validatedData = processMessages(messages);

        if (!validatedData.isEmpty()) {
            radarDataRepository.saveAll(validatedData);

            validatedData.forEach(data -> {
                FotoradarDataReceivedDto receivedDto = mapRadarDataToFotoradarDataReceivedDto(data);

                try {
                    String jsonData = serializeFotoradarDataReceivedDtoToJSON(receivedDto);
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

            if (checkIfDtoContainsNullValues(providedDto)) {
                // TODO minimum to jakiś komunikat w logach (uwzględnić anonimizację nie wysyłaj info o licensePlate) / slf4j
                logger.warn("Provided DTO contains null or invalid values: radarId={}, eventTimestamp={}, licensePlate=ANONYMIZED",
                        providedDto.radarId(), providedDto.eventTimestamp());
                return null;
            }
            int vehicleSpeedKMH = calculateSpeedKMH(providedDto.vehicleSpeed() , providedDto.speedUnit());
            return new RadarData(
                    providedDto.radarId(),
                    providedDto.eventTimestamp(),
                    providedDto.speedLimit(),
                    vehicleSpeedKMH,
                    providedDto.licensePlate(),
                    providedDto.imageUrl()
            );

        } catch (Exception e) {
            // TODO minimum to jakiś komunikat w logach (uwzględnić anonimizację nie wysyłaj info o licensePlate ) / slf4j
            logger.error("An error occurred while processing radar data: radarId={}, eventTimestamp={}, licensePlate=ANONYMIZED. Exception type: {}",
                    providedDto.radarId(), providedDto.eventTimestamp(), e.getClass().getName());
            return null;
        }
    }

    private List<RadarData> processMessages(List<FotoradarDataProvidedDto> messages){
        return messages.stream()
                .map(this::processFotoradarData)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private FotoradarDataReceivedDto mapRadarDataToFotoradarDataReceivedDto(RadarData radarData){
        return new FotoradarDataReceivedDto(
                radarData.getRadarExternalId(),
                radarData.getEventTimestamp(),
                radarData.getSpeedLimit(),
                radarData.getVehicleSpeed(),
                radarData.getLicensePlate(),
                radarData.getImageUrl()
        );
    }

    private String serializeFotoradarDataReceivedDtoToJSON(FotoradarDataReceivedDto receivedDto) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.writeValueAsString(receivedDto);
    }

    private int calculateSpeedKMH(int speed , String speedUnit){
        return speedUnit.equalsIgnoreCase("KMH")  ? speed : changeMPSToKMH(speed);
    }

    private int changeMPSToKMH(int speedMPS){
         return speedMPS * 36 / 10;
    }

    private boolean checkIfDtoContainsNullValues (FotoradarDataProvidedDto providedDto){
        return providedDto.radarId() == null || providedDto.radarId().isEmpty() ||
                providedDto.eventTimestamp() == null ||
                providedDto.licensePlate() == null || providedDto.licensePlate().isEmpty();
    }
}
