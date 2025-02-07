package com.autofine.fotoradar_data_service;
import com.autofine.fotoradar_data_service.config.KafkaTestConfig;
import com.autofine.fotoradar_data_service.dto.FotoradarDataProvidedDto;
import com.autofine.fotoradar_data_service.model.RadarData;
import com.autofine.fotoradar_data_service.repository.RadarDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@TestPropertySource(
        properties = {
                "spring.kafka.consumer.auto-offset-reset=earliest",
        }
)
@Import(KafkaTestConfig.class)
public class FotoradarDataServiceE2ETest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private RadarDataRepository radarDataRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    private static final String INPUT_TOPIC = "fotoradar.data.provided";
    private static final String OUTPUT_TOPIC = "fotoradar.data.received";

    @Test
    void whenBatchMessagesSent_thenProcessedAndSentToOutputTopic() throws Exception {
        // Stwórz 5 testowych wiadomości
        List<FotoradarDataProvidedDto> messages = IntStream.range(0, 5)
                .mapToObj(i -> new FotoradarDataProvidedDto(
                        "radar-" + i,
                        LocalDateTime.now(),
                        100 + i,
                        "ABC" + i,
                        "image" + i + ".jpg",
                        90,
                        "KMH"
                ))
                .toList();

        // Wyślij wiadomości do tematu wejściowego
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        for (FotoradarDataProvidedDto message : messages) {
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(INPUT_TOPIC, jsonMessage);
        }

        // Poczekaj max 30 sekund i sprawdź wyniki
        //służy do asynchronicznego czekania na spełnienie warunków testowych.
        //Jest to element biblioteki Awaitility, która pomaga testować asynchroniczne operacje.
        //Awaitility powtarza sprawdzanie asercji, aż będą poprawne lub do timeoutu
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            // Sprawdź liczbę rekordów w bazie danych
            List<RadarData> savedData = radarDataRepository.findAll();
            assertThat(savedData).hasSize(messages.size());

            // Sprawdź komunikaty w temacie wyjściowym
            Consumer<String, String> consumer = consumerFactory.createConsumer();
            consumer.subscribe(Collections.singleton(OUTPUT_TOPIC));

            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(5000)); // poniżej opis:
            //Pobiera wiadomości z Kafki z tematu, na który subskrybuje consumer (w moim przypadku fotoradar.data.received).
            //Czeka maksymalnie 5 sekund (Duration.ofMillis(5000)) na pojawienie się wiadomości.
            //Zwraca wszystkie znalezione rekordy w postaci obiektu ConsumerRecords<String, String>.
            //Jeśli w ciągu 5 sekund nie pojawią się żadne wiadomości, metoda zwróci pusty wynik.
            //KafkaTestUtils.getRecords(...): To pomocnicza metoda z Spring Kafka do symulowania konsumpcji wiadomości. Wykonuje "poll" na konsumencie, aby sprawdzić, czy są nowe rekordy w temacie.

            consumer.close();
            //Zamyka połączenie z Kafką
            //Zatrzymuje wątki wewnętrzne (np. heartbeat do Kafki).
            //Jeśli nie zamknę konsumenta, zasoby nie zostaną zwolnione

            // analogia do telefonu gdzie consumer to otwarte połączenie telefoniczne, a consumer.close() to odłożenie słuchawki – kończę rozmowę i zwalniam linię.
            //Bez tego: linia pozostaje zajęta, a inni nie mogą zadzwonić (brak dostępnych zasobów).

            assertThat(records.count()).isEqualTo(messages.size());
        });
    }
}
