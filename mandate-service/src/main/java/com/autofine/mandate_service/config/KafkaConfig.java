package com.autofine.mandate_service.config;

import com.autofine.mandate_service.model.dto.FotoradarDataReceivedDto;
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
    public ConsumerFactory<String, FotoradarDataReceivedDto> mandateDataConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autofine.*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(FotoradarDataReceivedDto.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FotoradarDataReceivedDto> batchKafkaListenerContainerFactory(ConsumerFactory<String, FotoradarDataReceivedDto> mandateDataConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, FotoradarDataReceivedDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(mandateDataConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
