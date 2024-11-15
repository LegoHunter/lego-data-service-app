package com.vattima.lego.data.service.config;

import com.vattima.lego.data.service.kafka.S3EventNotificationDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@EnableKafka
@Configuration
@Slf4j
public class KafkaConfiguration {

    private final AtomicLong counter = new AtomicLong(0);

    @KafkaListener(id = "chat-room", topics = "chat-room", groupId = "chat-room-group", concurrency = "4")
    public void listen(String in) {
        log.info("Received chat-room message {} : {}", counter.incrementAndGet(), in);
    }
//
//    @Bean
//    public ProducerFactory<String, S3EventNotification> awsS3EventProducerFactory(final KafkaProperties kafkaProperties) {
//        // ...
//        return new DefaultKafkaProducerFactory<>(Map.of(
//                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers()),
//                new StringSerializer(),
//                new S3EventNotificationSerializer());
//    }

    @Bean
    public ProducerFactory<String, String> awsS3EventProducerFactory(final KafkaProperties kafkaProperties) {
        // ...
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers()),
                new StringSerializer(),
                new StringSerializer());
    }

    @Bean
    public JsonMessageConverter converter() {
        return new JsonMessageConverter();
    }

    @Bean
    public KafkaListenerContainerFactory<?> kafkaJsonListenerContainerFactory(final KafkaProperties kafkaProperties) {
        ConcurrentKafkaListenerContainerFactory<String, S3EventNotification> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers()),
                new StringDeserializer(),
                new S3EventNotificationDeserializer()
        ));
        return factory;
    }

//    @Bean
//    public KafkaTemplate<String, S3EventNotification> awsS3EventKafkaTemplate(final ProducerFactory<String, S3EventNotification> awsS3EventProducerFactory) {
//        return new KafkaTemplate<>(awsS3EventProducerFactory);
//    }


    @Bean
    public KafkaTemplate<String, String> awsS3EventKafkaTemplate(final ProducerFactory<String, String> awsS3EventProducerFactory) {
        return new KafkaTemplate<>(awsS3EventProducerFactory);
    }


//    @KafkaListener(id = "aws-chat-room", topics = "aws-chat-room", groupId = "${groupId:aws-chat-room-group-1}", concurrency = "4")
//    public void awsListen(String in) {
//        log.info("Received message from AWS {} : {}", counter.incrementAndGet(), in);
//    }

}

