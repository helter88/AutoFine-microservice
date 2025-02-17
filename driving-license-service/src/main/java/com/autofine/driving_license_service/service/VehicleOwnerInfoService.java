package com.autofine.driving_license_service.service;

import com.autofine.driving_license_service.model.dto.VehicleOwnerInfoDto;
import com.autofine.driving_license_service.model.entity.DrivingLicense;
import com.autofine.driving_license_service.model.entity.VehicleOwner;
import com.autofine.driving_license_service.repository.DrivingLicenseRepository;
import com.autofine.driving_license_service.repository.VehicleOwnerRepository;
import org.springframework.stereotype.Service;

@Service
public class VehicleOwnerInfoService {
    private final VehicleOwnerRepository vehicleOwnerRepository;

    private final DrivingLicenseRepository drivingLicenseRepository;

    public VehicleOwnerInfoService(VehicleOwnerRepository vehicleOwnerRepository, DrivingLicenseRepository drivingLicenseRepository) {
        this.vehicleOwnerRepository = vehicleOwnerRepository;
        this.drivingLicenseRepository = drivingLicenseRepository;
    }

    public VehicleOwnerInfoDto getOwnerInfo(String licensePlate) {
        VehicleOwner vehicleOwner = vehicleOwnerRepository.findByLicensePlate(licensePlate);
        if ( vehicleOwner == null) return null;
        DrivingLicense drivingLicense = drivingLicenseRepository.findById(vehicleOwner.getUserId()).orElse(null);
        if (drivingLicense == null) return null;
        return new VehicleOwnerInfoDto(
                drivingLicense.getUserId(),
                vehicleOwner.getId(),
                drivingLicense.getPoints()
        );
    }
}
