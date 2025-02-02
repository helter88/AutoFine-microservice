package com.autofine.fotoradar_data_service.service;

import com.autofine.fotoradar_data_service.dto.FotoradarDataProvidedDto;
import com.autofine.fotoradar_data_service.dto.FotoradarDataReceivedDto;
import com.autofine.fotoradar_data_service.model.RadarData;
import com.autofine.fotoradar_data_service.repository.RadarDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FotoradarDataServiceTest {

    @Mock
    private RadarDataRepository radarDataRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private FotoradarDataService fotoradarDataService;

    @Test
    void processAndSendFotoradarData_validDto_shouldSaveToDatabaseAndPublishToKafka() throws JsonProcessingException {
        // Arrange
        FotoradarDataProvidedDto providedDto = new FotoradarDataProvidedDto(
                "radar123",
                LocalDateTime.now(),
                100,
                "ABC 123",
                "image.jpg",
                90,
                "KMH"
        );

        RadarData expectedRadarData = new RadarData(
                "radar123",
                providedDto.eventTimestamp(),
                90,
                100,
                "ABC 123",
                "image.jpg"
        );

        String TOPIC_RECEIVED = "fotoradar.data.received";

        FotoradarDataReceivedDto receivedDto = new FotoradarDataReceivedDto(
                "radar123",
                providedDto.eventTimestamp(),
                90,
                100,
                "ABC 123",
                "image.jpg"
        );

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Act
        fotoradarDataService.processAndSendFotoradarData(providedDto);

        // Assert
        ArgumentCaptor<RadarData> radarDataCaptor = ArgumentCaptor.forClass(RadarData.class);
        verify(radarDataRepository, times(1)).save(radarDataCaptor.capture());
        RadarData capturedData = radarDataCaptor.getValue();
        assertThat(capturedData.getRadarExternalId()).isEqualTo(providedDto.radarId());
        assertThat(capturedData.getVehicleSpeed()).isEqualTo(providedDto.vehicleSpeed());

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC_RECEIVED);

        assertThat(messageCaptor.getValue()).isEqualTo(objectMapper.writeValueAsString(receivedDto));
    }

    @Test
    void processAndSendFotoradarData_nonValidDto_shouldSaveToDatabaseAndPublishToKafka() throws JsonProcessingException {
        // Arrange
        FotoradarDataProvidedDto providedDtoWithNull = new FotoradarDataProvidedDto(
                null,
                LocalDateTime.now(),
                100,
                "ABC 123",
                "image.jpg",
                90,
                "KMH"
        );
        fotoradarDataService.processAndSendFotoradarData(providedDtoWithNull);
        verify(radarDataRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void processAndSendFotoradarData_speedUnitIsMPS_shouldConvertSpeedToKMH() throws Exception {
        // Arrange
        FotoradarDataProvidedDto providedDto = new FotoradarDataProvidedDto(
                "radar123",
                LocalDateTime.now(),
                10,
                "ABC 123",
                "image.jpg",
                90,
                "MPS"
        );

        // Act
        fotoradarDataService.processAndSendFotoradarData(providedDto);

        //Assert
        ArgumentCaptor<RadarData> radarDataCaptor = ArgumentCaptor.forClass(RadarData.class);
        verify(radarDataRepository, times(1)).save(radarDataCaptor.capture());
        RadarData capturedData = radarDataCaptor.getValue();
        assertThat(capturedData.getVehicleSpeed()).isEqualTo(36);
    }
}