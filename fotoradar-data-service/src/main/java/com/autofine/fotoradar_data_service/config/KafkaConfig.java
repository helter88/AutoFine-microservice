package com.autofine.fotoradar_data_service.config;

import com.autofine.fotoradar_data_service.dto.FotoradarDataProvidedDto;
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
    public ConsumerFactory<String, FotoradarDataProvidedDto> fotoradarDataConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.autofine.*");
        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), new JsonDeserializer<>(FotoradarDataProvidedDto.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, FotoradarDataProvidedDto> batchKafkaListenerContainerFactory(ConsumerFactory<String, FotoradarDataProvidedDto> fotoradarDataConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, FotoradarDataProvidedDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(fotoradarDataConsumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
