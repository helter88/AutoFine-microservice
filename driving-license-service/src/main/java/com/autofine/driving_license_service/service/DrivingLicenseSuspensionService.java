package com.autofine.driving_license_service.service;

import com.autofine.driving_license_service.kafka.DrivingLicenseProducer;
import com.autofine.driving_license_service.model.dto.MandateCreatedDto;
import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.entity.DrivingLicenseEvent;
import com.autofine.driving_license_service.model.enums.LicenseEventType;
import com.autofine.driving_license_service.model.enums.LicenseStatus;
import com.autofine.driving_license_service.repository.DrivingLicenseEventRepository;
import com.autofine.driving_license_service.repository.DrivingLicenseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class DrivingLicenseSuspensionService {

    private static final Logger logger = LoggerFactory.getLogger(DrivingLicenseSuspensionService.class);
    private final DrivingLicenseRepository drivingLicenseRepository;

    private final DrivingLicenseEventRepository drivingLicenseEventRepository;

    private final DrivingLicenseProducer drivingLicenseProducer;

    public DrivingLicenseSuspensionService(DrivingLicenseRepository drivingLicenseRepository, DrivingLicenseEventRepository drivingLicenseEventRepository, DrivingLicenseProducer drivingLicenseProducer) {
        this.drivingLicenseRepository = drivingLicenseRepository;
        this.drivingLicenseEventRepository = drivingLicenseEventRepository;
        this.drivingLicenseProducer = drivingLicenseProducer;
    }

    @Async("drivingLicenseDataExecutor")
    public void processMandateData(MandateCreatedDto data) throws JsonProcessingException {
        if(!data.pointsLimitExceeded()) return;
        DrivingLicense userDrivingLicence = drivingLicenseRepository.findById(data.userExternalId()).orElseGet(() ->{
            logger.info("No user found id: " + data.userExternalId());
            return null;
                }
        );
        if (userDrivingLicence == null) return;
        changeStatusToSuspended(userDrivingLicence, data.points());
        saveEvent(userDrivingLicence.getUserId());
        drivingLicenseProducer.sendLicenseSuspendedEvent(userDrivingLicence.getUserId());
    }
    @Transactional
    private void changeStatusToSuspended(DrivingLicense userDrivingLicence, int points) {
        LocalDate now = LocalDate.now();
        userDrivingLicence.setStatus(LicenseStatus.SUSPENDED);
        userDrivingLicence.setSuspensionStartDate(now);
        userDrivingLicence.setSuspensionEndDate(now.plusMonths(3));
        userDrivingLicence.setPoints(userDrivingLicence.getPoints() + points);
        drivingLicenseRepository.save(userDrivingLicence);
    }
    @Transactional
    private void saveEvent( UUID userId){
        DrivingLicenseEvent drivingLicenseEvent = new DrivingLicenseEvent(
              userId,
                LicenseEventType.SUSPENDED,
                LocalDateTime.now(),
                "Exceeding the allowed number of penalty points"
        );
        drivingLicenseEventRepository.save(drivingLicenseEvent);
    }
}
