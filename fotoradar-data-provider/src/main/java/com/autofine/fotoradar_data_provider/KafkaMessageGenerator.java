package com.autofine.fotoradar_data_provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Component
public class KafkaMessageGenerator implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageGenerator.class);

    private final KafkaTemplate<String, FotoradarDataProvidedDto> kafkaTemplate;

    public KafkaMessageGenerator(KafkaTemplate<String, FotoradarDataProvidedDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        String topic = "fotoradar.data.provided";
        int numberOfMessages = 100; // Zdefiniuj liczbę wiadomości do wysłania

        Random random = new Random();
        List<Integer> possibleSpeedLimits = Arrays.asList(40, 50, 70, 90, 120);

        for (int i = 0; i < numberOfMessages; i++) {
            String radarId = "RADAR_" + random.nextInt(10);
            LocalDateTime eventTimestamp = LocalDateTime.now().minusMinutes(random.nextInt(60)); // Losowe zdarzenie w ciągu ostatniej godziny
            int vehicleSpeed = 50 + random.nextInt(150); // Prędkość między 50 a 200 km/h
            String licensePlate = generateRandomLicensePlate();
            String imageUrl = "http://example.com/image/" + UUID.randomUUID() + ".jpg";
            int speedLimit = possibleSpeedLimits.get(random.nextInt(possibleSpeedLimits.size()));

            FotoradarDataProvidedDto data = new FotoradarDataProvidedDto(radarId, eventTimestamp, vehicleSpeed, licensePlate, imageUrl, speedLimit);

            kafkaTemplate.send(topic, data);
            logger.info("Wysłano wiadomość do tematu {}: {}", topic, data);

        }

        logger.info("Wysłano {} wiadomości do tematu {}", numberOfMessages, topic);
    }

    private String generateRandomLicensePlate() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append((char) ('A' + random.nextInt(26)));
        }
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
