package com.autofine.driving_license_service.config;

import com.autofine.driving_license_service.model.dto.MandateCreatedDto;
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
    public ConsumerFactory<String, MandateCreatedDto> mandateDataConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autofine.*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(MandateCreatedDto.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MandateCreatedDto> batchKafkaListenerContainerFactory(ConsumerFactory<String, MandateCreatedDto> driveingLicenseDataConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MandateCreatedDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(driveingLicenseDataConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
