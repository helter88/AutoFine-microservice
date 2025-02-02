package com.autofine.fotoradar_data_service.repository;

import com.autofine.fotoradar_data_service.model.RadarData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RadarDataRepositoryIT {
    @Container
    @ServiceConnection // Zastępuje starą konfiguracje @DynamicPropertySource
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RadarDataRepository repository;

    @Test
    void shouldSaveAndRetrieveRadarData() {
        // Given
        RadarData data = new RadarData(
                "radar-123",
                LocalDateTime.now(),
                50,
                60,
                "ABC123",
                "http://example.com/image.jpg"
        );

        // When
        RadarData saved = repository.save(data);
        Optional<RadarData> found = repository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getRadarExternalId()).isEqualTo("radar-123");
        assertThat(found.get().getVehicleSpeed()).isEqualTo(60);
    }

    @Test
    void shouldFindAllRadarData() {
        // Arrange
        RadarData radarData1 = new RadarData("radar1", LocalDateTime.now(), 100, 120, "ABC 123", "image1.jpg");
        RadarData radarData2 = new RadarData("radar2", LocalDateTime.now(), 100, 120, "DEF 456", "image2.jpg");
        repository.save(radarData1);
        repository.save(radarData2);
        // Act
        List<RadarData> foundRadarData = repository.findAll();
        // Assert
        assertThat(foundRadarData).hasSize(2);
        assertThat(foundRadarData).extracting(RadarData::getRadarExternalId).contains("radar1", "radar2");
    }
}
