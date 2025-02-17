package com.autofine.driving_license_service.repository;

import com.autofine.driving_license_service.model.entity.DrivingLicenseEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DrivingLicenseEventRepository extends JpaRepository<DrivingLicenseEvent, UUID> {
}
