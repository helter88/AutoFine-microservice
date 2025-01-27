package com.autofine.fotoradar_data_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.kafka.listener.concurrency=1",
        "spring.kafka.listener.batch-size=1"
})
public class FotoradarDataServiceE2ESingleThreadTest extends FotoradarDataServiceE2ETest {
    @Test
    void whenSingleThreaded_processingTimeIsMeasured() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        super.whenBatchMessagesSent_thenProcessedAndSentToOutputTopic();

        stopWatch.stop();
        System.out.println("Processing time (single-threaded): " + stopWatch.getTotalTimeMillis() + " ms");
    }
}
