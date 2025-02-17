package com.autofine.driving_license_service.service;

import com.autofine.driving_license_service.kafka.DrivingLicenseProducer;
import com.autofine.driving_license_service.model.dto.MandateCreatedDto;
import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.entity.DrivingLicenseEvent;
import com.autofine.driving_license_service.model.enums.LicenseStatus;
import com.autofine.driving_license_service.repository.DrivingLicenseEventRepository;
import com.autofine.driving_license_service.repository.DrivingLicenseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrivingLicenseSuspensionServiceTest {
    @Mock
    private DrivingLicenseRepository drivingLicenseRepository;

    @Mock
    private DrivingLicenseEventRepository drivingLicenseEventRepository;

    @Mock
    private DrivingLicenseProducer drivingLicenseProducer;

    @InjectMocks
    private DrivingLicenseSuspensionService service;

    @Test
    void shouldSuspendLicenseWhenPointsExceeded() throws JsonProcessingException {
        // Given
        UUID userId = UUID.randomUUID();
        MandateCreatedDto data = new MandateCreatedDto(
                UUID.randomUUID(),
                userId,
                new BigDecimal("300.50"),
                LocalDateTime.now(),
                UUID.randomUUID(),
                20,
                true
        );

        DrivingLicense license = new DrivingLicense(userId, LicenseStatus.VALID, null, null);
        license.setPoints(5);

        when(drivingLicenseRepository.findById(userId)).thenReturn(Optional.of(license));

        // When
        service.processMandateData(data);

        // Then
        ArgumentCaptor<DrivingLicense> licenseCaptor = ArgumentCaptor.forClass(DrivingLicense.class);
        verify(drivingLicenseRepository).save(licenseCaptor.capture());

        DrivingLicense updatedLicense = licenseCaptor.getValue();
        assertEquals(LicenseStatus.SUSPENDED, updatedLicense.getStatus());
        assertEquals(LocalDate.now(), updatedLicense.getSuspensionStartDate());
        assertEquals(LocalDate.now().plusMonths(3), updatedLicense.getSuspensionEndDate());
        assertEquals(25, updatedLicense.getPoints());

        verify(drivingLicenseEventRepository).save(any(DrivingLicenseEvent.class));
        verify(drivingLicenseProducer).sendLicenseSuspendedEvent(userId);
    }

    @Test
    void shouldNotProcessWhenPointsNotExceeded() throws JsonProcessingException {
        // Given
        MandateCreatedDto data = new MandateCreatedDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                LocalDateTime.now(),
                UUID.randomUUID(),
                5,
                false
        );

        // When
        service.processMandateData(data);

        // Then
        verifyNoInteractions(drivingLicenseRepository);
        verifyNoInteractions(drivingLicenseEventRepository);
        verifyNoInteractions(drivingLicenseProducer);
    }

    @Test
    void shouldNotProcessIfDrivingLicenseNotExists() throws JsonProcessingException {
        // Given
        UUID newUserId = UUID.randomUUID();
        MandateCreatedDto data = new MandateCreatedDto(
                UUID.randomUUID(),
                newUserId,
                new BigDecimal("500.00"),
                LocalDateTime.now(),
                UUID.randomUUID(),
                25,
                true
        );

        when(drivingLicenseRepository.findById(newUserId)).thenReturn(Optional.empty());

        // When
        service.processMandateData(data);

        // Then
        verify(drivingLicenseRepository, never()).save(any());
        verifyNoInteractions(drivingLicenseEventRepository);
        verifyNoInteractions(drivingLicenseProducer);
    }
}