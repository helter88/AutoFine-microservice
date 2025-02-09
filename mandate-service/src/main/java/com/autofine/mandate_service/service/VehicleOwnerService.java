package com.autofine.mandate_service.service;

import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
@Service
public class VehicleOwnerService {
    public VehicleOwnerInfo getVehicleOwnerInfo(String licensePlate) {
        // Dummy implementation
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        int ownerPoints = generatePoints();
        return new VehicleOwnerInfo(ownerId, vehicleId, ownerPoints);
    }

    private int generatePoints() {
        var rd = new Random();
        return rd.nextInt(0,25);
    }

    public record VehicleOwnerInfo(UUID ownerId, UUID vehicleId, int pointsNumber) {}
}
