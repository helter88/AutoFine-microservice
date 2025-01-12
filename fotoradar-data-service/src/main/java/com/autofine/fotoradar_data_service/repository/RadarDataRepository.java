package com.autofine.fotoradar_data_service.repository;

import com.autofine.fotoradar_data_service.model.RadarData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RadarDataRepository extends JpaRepository<RadarData, UUID> {
}
