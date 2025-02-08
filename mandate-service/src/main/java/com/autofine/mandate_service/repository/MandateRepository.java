package com.autofine.mandate_service.repository;

import com.autofine.mandate_service.model.entity.Mandate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MandateRepository extends JpaRepository<Mandate, UUID> {
}