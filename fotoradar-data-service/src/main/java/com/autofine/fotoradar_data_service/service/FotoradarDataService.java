package com.autofine.fotoradar_data_service.service;

import com.autofine.fotoradar_data_service.model.RadarData;
import com.autofine.fotoradar_data_service.dto.FotoradarDataProvidedDto;
import com.autofine.fotoradar_data_service.dto.FotoradarDataReceivedDto;
import com.autofine.fotoradar_data_service.repository.RadarDataRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FotoradarDataService {

    private final RadarDataRepository radarDataRepository;
    private final KafkaTemplate<String, FotoradarDataReceivedDto> kafkaTemplate;
    private static final String TOPIC_RECEIVED = "fotoradar.data.received";

    public FotoradarDataService(RadarDataRepository radarDataRepository, KafkaTemplate<String, FotoradarDataReceivedDto> kafkaTemplate) {
        this.radarDataRepository = radarDataRepository;
        this.kafkaTemplate = kafkaTemplate;
    }


    @KafkaListener(topics = "fotoradar.data.provided", groupId = "fotoradar-data-group", containerFactory = "batchKafkaListenerContainerFactory")
    public void receiveFotoradarDataBatch(List<FotoradarDataProvidedDto> messages) {
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
                kafkaTemplate.send(TOPIC_RECEIVED, receivedDto);
            });
        }
    }

    private RadarData processFotoradarData(FotoradarDataProvidedDto providedDto) {
        try {

            if (providedDto.radarId() == null || providedDto.radarId().isEmpty() ||
                    providedDto.eventTimestamp() == null ||
                    providedDto.licensePlate() == null || providedDto.licensePlate().isEmpty()) {
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
            return null;
        }
    }
}
