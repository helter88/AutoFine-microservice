package com.autofine.driving_license_service.repository;

import com.autofine.driving_license_service.model.entity.VehicleOwner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VehicleOwnerRepository extends JpaRepository<VehicleOwner, UUID> {
    VehicleOwner findByLicensePlate(String licensePlate);
}
