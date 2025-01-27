package com.autofine.fotoradar_data_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.kafka.listener.concurrency=5",
        "spring.kafka.listener.batch-size=10"
})
public class FotoradarDataServiceE2EMultiThreadTest extends FotoradarDataServiceE2ETest {
    @Test
    void whenMultiThreaded_processingTimeIsFaster() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        super.whenBatchMessagesSent_thenProcessedAndSentToOutputTopic();

        stopWatch.stop();
        System.out.println("Processing time (multi-threaded): " + stopWatch.getTotalTimeMillis() + " ms");
    }
}
