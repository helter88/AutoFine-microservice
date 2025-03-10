package com.autofine.driving_license_service.controller;

import com.autofine.driving_license_service.model.dto.VehicleOwnerInfoDto;
import com.autofine.driving_license_service.service.VehicleOwnerInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicleOwner")
public class VehicleOwnerController {

    private final VehicleOwnerInfoService vehicleOwnerInfoService;

    public VehicleOwnerController(VehicleOwnerInfoService vehicleOwnerInfoService) {
        this.vehicleOwnerInfoService = vehicleOwnerInfoService;
    }

    @GetMapping("/{licensePlate}")
    public VehicleOwnerInfoDto getOwnerInfo(String licensePlate) {
        return vehicleOwnerInfoService.getOwnerInfo(licensePlate);
    }
}
