package com.autofine.mandate_service.kafka;

import com.autofine.mandate_service.model.dto.FotoradarDataReceivedDto;
import com.autofine.mandate_service.service.MandateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class RadarDataConsumer {
    private final MandateService mandateService;
    @KafkaListener(topics = "fotoradar.data.received", groupId = "mandate-data-group", containerFactory = "batchKafkaListenerContainerFactory")
    public void receiveFotoradarDataBatch(List<FotoradarDataReceivedDto> messages) {
        log.info("Received a batch of {} messages", messages.size());
        messages.parallelStream().forEach(mandateService::processFotoradarData);
    }
}
