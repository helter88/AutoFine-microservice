package com.autofine.mandate_service.service;

import com.autofine.mandate_service.model.dto.FotoradarDataReceivedDto;
import com.autofine.mandate_service.model.dto.MandateCreatedDto;
import com.autofine.mandate_service.model.entity.Mandate;
import com.autofine.mandate_service.model.enums.PenaltyRate;
import com.autofine.mandate_service.model.enums.PointRate;
import com.autofine.mandate_service.repository.MandateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MandateServiceTest {
    @Mock
    private MandateRepository mandateRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private VehicleOwnerService vehicleOwnerService;

    @InjectMocks
    private MandateService mandateService;
    private static final String MANDATE_CREATED_TOPIC = "mandate.created";

    @Test
    public void processFotoradarData_NoExcess_NoMandateCreated() {
        FotoradarDataReceivedDto noExcessData = new FotoradarDataReceivedDto(
                UUID.randomUUID(),
                LocalDateTime.now(),
                100,
                90,
                "ABC-123",
                "image.jpg"
        );

        mandateService.processFotoradarData(noExcessData);

        verifyNoInteractions(mandateRepository, kafkaTemplate);
    }

    @Test
    public void processFotoradarData_UserNotFound_NoMandateCreated() {
        FotoradarDataReceivedDto data = new FotoradarDataReceivedDto(
                UUID.randomUUID(),
                LocalDateTime.now(),
                50,
                70,
                "ABC-123",
                "image.jpg"
        );

        when(vehicleOwnerService.getVehicleOwnerInfo(data.licensePlate())).thenReturn(null);

        mandateService.processFotoradarData(data);

        verifyNoInteractions(mandateRepository, kafkaTemplate);
    }


    @Test
    public void processFotoradarData_ValidData_MandateCreatedAndEventPublished() {
        // Given
        UUID radarId = UUID.randomUUID();
        LocalDateTime eventTime = LocalDateTime.now();
        int speedLimit = 50;
        int vehicleSpeed = 65;
        String licensePlate = "ABC-123";
        String imageUrl = "image.jpg";
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        int initialPoints = 10;

        FotoradarDataReceivedDto data = new FotoradarDataReceivedDto(
                radarId,
                eventTime,
                speedLimit,
                vehicleSpeed,
                licensePlate,
                imageUrl);

        VehicleOwnerService.VehicleOwnerInfo ownerInfo = new VehicleOwnerService.VehicleOwnerInfo(ownerId, vehicleId, initialPoints);

        when(vehicleOwnerService.getVehicleOwnerInfo(licensePlate)).thenReturn(ownerInfo);
        when(mandateRepository.save(any(Mandate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<MandateCreatedDto> mandateCreatedCaptor = ArgumentCaptor.forClass(MandateCreatedDto.class);

        // When
        mandateService.processFotoradarData(data);

        // Then
        verify(mandateRepository).save(any(Mandate.class));
        verify(kafkaTemplate).send(eq(MANDATE_CREATED_TOPIC), mandateCreatedCaptor.capture());

        MandateCreatedDto capturedEvent = mandateCreatedCaptor.getValue();
        assertNotNull(capturedEvent);
        assertEquals(ownerId, capturedEvent.userExternalId());
        assertEquals(vehicleId, capturedEvent.vehicleExternalId());
        assertEquals(initialPoints + PointRate.FROM_11_TO_15.getPoints() >= 24, capturedEvent.pointsLimitExceeded());

        BigDecimal expectedFine = PenaltyRate.FROM_11_TO_15.getAmount();
        assertEquals(expectedFine, capturedEvent.fineAmount());
    }
}