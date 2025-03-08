package com.autofine.driving_license_service.service;

import com.autofine.driving_license_service.kafka.DrivingLicenseProducer;
import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.entity.DrivingLicenseEvent;
import com.autofine.driving_license_service.model.enums.LicenseEventType;
import com.autofine.driving_license_service.model.enums.LicenseStatus;
import com.autofine.driving_license_service.repository.DrivingLicenseEventRepository;
import com.autofine.driving_license_service.repository.DrivingLicenseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DrivingLicenseReinstatementService {
    private static final Logger logger = LoggerFactory.getLogger(DrivingLicenseSuspensionService.class);
    private final DrivingLicenseRepository drivingLicenseRepository;
    private final DrivingLicenseEventRepository drivingLicenseEventRepository;
    private final DrivingLicenseProducer drivingLicenseProducer;

    public DrivingLicenseReinstatementService(DrivingLicenseRepository drivingLicenseRepository, DrivingLicenseEventRepository drivingLicenseEventRepository, DrivingLicenseProducer drivingLicenseProducer) {
        this.drivingLicenseRepository = drivingLicenseRepository;
        this.drivingLicenseEventRepository = drivingLicenseEventRepository;
        this.drivingLicenseProducer = drivingLicenseProducer;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight  // podobnie jak robisz pakiet kafka, tak na crony można zrobić osobny, z minimalistyczną metodą wołającą tę
    public void checkForLicenseReinstatement() {
        List<DrivingLicense> licensesToReinstate = drivingLicenseRepository
                .findAllByStatusAndSuspensionEndDate(LicenseStatus.SUSPENDED, LocalDate.now());

        licensesToReinstate.forEach(license -> {
            license.setStatus(LicenseStatus.VALID);
            license.setSuspensionStartDate(null);
            license.setSuspensionEndDate(null);
            drivingLicenseRepository.save(license);
            saveEvent(license.getUserId());

            try {
                drivingLicenseProducer.sendLicenseReinstatedEvent(license.getUserId());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            logger.info("License reinstated for user: {}", license.getUserId());
        });
    }

    @Transactional
    private void saveEvent( UUID userId){
        DrivingLicenseEvent drivingLicenseEvent = new DrivingLicenseEvent(
                userId,
                LicenseEventType.REINSTATED,
                LocalDateTime.now(),
                "Driving licence reinstated"
        );
        drivingLicenseEventRepository.save(drivingLicenseEvent);
    }
}
