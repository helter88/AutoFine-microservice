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
    private final ObjectMapper objectMapper;

    public FotoradarDataService(RadarDataRepository radarDataRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.radarDataRepository = radarDataRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }


    @KafkaListener(topics = "fotoradar.data.provided", groupId = "fotoradar-data-group", containerFactory = "batchKafkaListenerContainerFactory")
    public void receiveFotoradarDataBatch(List<FotoradarDataProvidedDto> messages) {
        logger.info("Received a batch of {} messages", messages.size());
        messages.parallelStream().forEach(this::processAndSendFotoradarData);
    }

    @Async("fotoradarDataExecutor")
    public void processAndSendFotoradarData(FotoradarDataProvidedDto providedDto) {
        RadarData radarData = processFotoradarData(providedDto);
        if (radarData != null) {
            radarDataRepository.save(radarData);
            FotoradarDataReceivedDto receivedDto = mapRadarDataToFotoradarDataReceivedDto(radarData);
            try {
                String jsonData = serializeFotoradarDataReceivedDtoToJSON(receivedDto);
                kafkaTemplate.send(TOPIC_RECEIVED, jsonData);
                logger.info("Wysłano wiadomość do tematu {}: {}", TOPIC_RECEIVED, jsonData);
            } catch (JsonProcessingException e) {
                logger.error("Błąd podczas serializacji do JSON", e);
            }
        }
    }

    private RadarData processFotoradarData(FotoradarDataProvidedDto providedDto) {
        try {

            if (checkIfDtoContainsNullValues(providedDto)) {
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
            logger.error("An error occurred while processing radar data: radarId={}, eventTimestamp={}, licensePlate=ANONYMIZED. Exception type: {}",
                    providedDto.radarId(), providedDto.eventTimestamp(), e.getClass().getName());
            return null;
        }
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
