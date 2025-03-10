package com.autofine.fotoradar_data_provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaMessageGenerator implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageGenerator.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    public KafkaMessageGenerator(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void run(String... args) throws Exception {
        String topic = "fotoradar.data.provided";
        int numberOfMessages = 1000;
        int numberOfThreads = 10;

        List<Integer> possibleSpeedLimits = Arrays.asList(40, 50, 70, 90, 120);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfMessages; i++) {
            executorService.submit(() -> {
                FotoradarDataProvidedDto data = generateFotoradarDataProviderDto(possibleSpeedLimits);
                String jsonData = null;
                try {
                    jsonData = serializeDtoData(data);
                    kafkaTemplate.send(topic, jsonData);
                    logger.info("Wysłano wiadomość do tematu {}: {}", topic, jsonData);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                logger.warn("Nie wszystkie zadania zakończyły się przed upływem czasu.");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Oczekiwanie na zakończenie wątków zostało przerwane.", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
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

    private int generatePossibleSpeedLimit(List<Integer> possibleSpeedLimits, Random random) {
        return possibleSpeedLimits.get(random.nextInt(possibleSpeedLimits.size()));
    }

    private FotoradarDataProvidedDto generateFotoradarDataProviderDto(List<Integer> possibleSpeedLimits) {
        Random random = new Random();
        String radarId = "RADAR_" + random.nextInt(10);
        LocalDateTime eventTimestamp = LocalDateTime.now().minusMinutes(random.nextInt(60)); // Losowe zdarzenie w ciągu ostatniej godziny
        int vehicleSpeed = 50 + random.nextInt(150); // Prędkość między 50 a 199 km/h
        String licensePlate = generateRandomLicensePlate();
        String imageUrl = "http://example.com/image/" + UUID.randomUUID() + ".jpg";
        int speedLimit = generatePossibleSpeedLimit(possibleSpeedLimits, random);
        Unit unit = getRandomUnit(random);

        return  new FotoradarDataProvidedDto(radarId, eventTimestamp, vehicleSpeed, licensePlate, imageUrl, speedLimit, unit);
    }

    private Unit getRandomUnit(Random random) {
        Unit[] units = Unit.values();
        int randomIndex = random.nextInt(units.length);
        return units[randomIndex];
    }

    private String serializeDtoData(FotoradarDataProvidedDto data) throws JsonProcessingException {
        return objectMapper.writeValueAsString(data);
    }
}
