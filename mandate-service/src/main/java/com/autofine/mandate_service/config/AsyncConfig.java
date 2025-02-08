package com.autofine.mandate_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "mandateDataExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // Liczba wątków w puli
        executor.setMaxPoolSize(10); // Maksymalna liczba wątków
        executor.setQueueCapacity(100); // Pojemność kolejki zadań
        executor.setThreadNamePrefix("AsyncProcess-");
        executor.initialize();
        return executor;
    }
}
