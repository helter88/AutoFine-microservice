package com.autofine.driving_license_service.repository;

import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.enums.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DrivingLicenseRepository extends JpaRepository<DrivingLicense, UUID> {

    List<DrivingLicense> findAllByStatusAndSuspensionEndDate(LicenseStatus status, LocalDate date);
}
