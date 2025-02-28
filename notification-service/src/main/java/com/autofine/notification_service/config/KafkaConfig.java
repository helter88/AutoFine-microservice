package com.autofine.notification_service.config;

import com.autofine.notification_service.model.dto.DrivingLicenseReinstatedDto;
import com.autofine.notification_service.model.dto.DrivingLicenseSuspendedDto;
import com.autofine.notification_service.model.dto.MandateCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    public ConsumerFactory<String, MandateCreatedEvent> mandateCreatedDataConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autofine.*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(MandateCreatedEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MandateCreatedEvent> batchKafkaMandateCreatedContainerFactory(ConsumerFactory<String, MandateCreatedEvent> mandateCreatedDataConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MandateCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(mandateCreatedDataConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DrivingLicenseSuspendedDto> licenseSuspendedDataConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autofine.*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(DrivingLicenseSuspendedDto.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DrivingLicenseSuspendedDto> batchKafkaLicenseSuspendedContainerFactory(ConsumerFactory<String, DrivingLicenseSuspendedDto> licenseSuspendedDataConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DrivingLicenseSuspendedDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(licenseSuspendedDataConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DrivingLicenseReinstatedDto> licenseReinstatedDataConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autofine.*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(DrivingLicenseReinstatedDto.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DrivingLicenseReinstatedDto> batchKafkaLicenseReinstateContainerFactory(ConsumerFactory<String, DrivingLicenseReinstatedDto> licenseReinstatedDataConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DrivingLicenseReinstatedDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(licenseReinstatedDataConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
