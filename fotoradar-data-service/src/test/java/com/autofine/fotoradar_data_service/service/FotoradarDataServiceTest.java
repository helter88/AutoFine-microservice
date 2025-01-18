package com.autofine.fotoradar_data_service.service;

import com.autofine.fotoradar_data_service.dto.FotoradarDataProvidedDto;
import com.autofine.fotoradar_data_service.dto.FotoradarDataReceivedDto;
import com.autofine.fotoradar_data_service.model.RadarData;
import com.autofine.fotoradar_data_service.repository.RadarDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
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
    void receiveFotoradarDataBatch_validMessages_shouldSaveToDatabaseAndPublishToKafka() throws Exception {
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
        List<FotoradarDataProvidedDto> messages = Collections.singletonList(providedDto);

        // Act
        fotoradarDataService.receiveFotoradarDataBatch(messages);

        // Assert
        ArgumentCaptor<List<RadarData>> radarDataCaptor = ArgumentCaptor.forClass(List.class);
        verify(radarDataRepository, times(1)).saveAll(radarDataCaptor.capture());
        List<RadarData> savedRadarData = radarDataCaptor.getValue();
        assertThat(savedRadarData).hasSize(1);
        assertThat(savedRadarData.get(0).getRadarExternalId()).isEqualTo("radar123");

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), messageCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo("fotoradar.data.received");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        FotoradarDataReceivedDto expectedDto = new FotoradarDataReceivedDto(
                "radar123",
                providedDto.eventTimestamp(),
                90,
                100,
                "ABC 123",
                "image.jpg"
        );
        assertThat(messageCaptor.getValue()).isEqualTo(objectMapper.writeValueAsString(expectedDto));
    }

    @Test
    void receiveFotoradarDataBatch_invalidMessages_shouldNotSaveToDatabaseOrPublishToKafka() {
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
        List<FotoradarDataProvidedDto> messages = Collections.singletonList(providedDtoWithNull);

        // Act
        fotoradarDataService.receiveFotoradarDataBatch(messages);

        // Assert
        verify(radarDataRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void receiveFotoradarDataBatch_speedUnitIsKMH_shouldSaveSpeedDirectly() throws Exception {
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
        List<FotoradarDataProvidedDto> messages = Collections.singletonList(providedDto);

        // Act
        fotoradarDataService.receiveFotoradarDataBatch(messages);

        // Assert
        ArgumentCaptor<List<RadarData>> radarDataCaptor = ArgumentCaptor.forClass(List.class);
        verify(radarDataRepository, times(1)).saveAll(radarDataCaptor.capture());
        List<RadarData> savedRadarData = radarDataCaptor.getValue();
        assertThat(savedRadarData).hasSize(1);
        assertThat(savedRadarData.get(0).getVehicleSpeed()).isEqualTo(100); // Sprawdzamy, czy prędkość została zapisana bez zmian
    }

    @Test
    void receiveFotoradarDataBatch_speedUnitIsMPS_shouldConvertSpeedToKMH() throws Exception {
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
        List<FotoradarDataProvidedDto> messages = Collections.singletonList(providedDto);

        // Act
        fotoradarDataService.receiveFotoradarDataBatch(messages);

        // Assert
        ArgumentCaptor<List<RadarData>> radarDataCaptor = ArgumentCaptor.forClass(List.class);
        verify(radarDataRepository, times(1)).saveAll(radarDataCaptor.capture());
        List<RadarData> savedRadarData = radarDataCaptor.getValue();
        assertThat(savedRadarData).hasSize(1);
        assertThat(savedRadarData.get(0).getVehicleSpeed()).isEqualTo(36); // Sprawdzamy, czy prędkość została poprawnie przeliczona
    }
}