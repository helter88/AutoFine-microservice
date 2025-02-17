package com.autofine.driving_license_service.service;

import com.autofine.driving_license_service.kafka.DrivingLicenseProducer;
import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.entity.DrivingLicenseEvent;
import com.autofine.driving_license_service.model.enums.LicenseStatus;
import com.autofine.driving_license_service.repository.DrivingLicenseEventRepository;
import com.autofine.driving_license_service.repository.DrivingLicenseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrivingLicenseReinstatementServiceTest {
    @Mock
    private DrivingLicenseRepository drivingLicenseRepository;

    @Mock
    private DrivingLicenseEventRepository drivingLicenseEventRepository;

    @Mock
    private DrivingLicenseProducer drivingLicenseProducer;

    @InjectMocks
    private DrivingLicenseReinstatementService service;

    @Test
    void shouldReinstateEligibleLicenses() throws JsonProcessingException {
        // Given
        UUID userId = UUID.randomUUID();
        DrivingLicense license = new DrivingLicense(
                userId,
                LicenseStatus.SUSPENDED,
                LocalDate.now().minusMonths(3),
                LocalDate.now()
        );

        when(drivingLicenseRepository.findAllByStatusAndSuspensionEndDate(
                LicenseStatus.SUSPENDED,
                LocalDate.now()
        )).thenReturn(List.of(license));

        // When
        service.checkForLicenseReinstatement();

        // Then
        assertEquals(LicenseStatus.VALID, license.getStatus());
        assertNull(license.getSuspensionStartDate());
        assertNull(license.getSuspensionEndDate());

        verify(drivingLicenseRepository).save(license);
        verify(drivingLicenseEventRepository).save(any(DrivingLicenseEvent.class));
        verify(drivingLicenseProducer).sendLicenseReinstatedEvent(userId);
    }
}