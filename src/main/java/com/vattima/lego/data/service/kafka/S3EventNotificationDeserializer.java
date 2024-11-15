package com.vattima.lego.data.service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

import java.util.Collection;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class S3EventNotificationDeserializer implements Deserializer<S3EventNotification> {

    @Override
    public S3EventNotification deserialize(String topic, Headers headers, byte[] data) {
        try {
            S3EventNotification s3EventNotification = S3EventNotification.fromJson(data);
            log.info("Deserialized {} to {}", new String(data), s3EventNotification);

            Stream.ofNullable(s3EventNotification.getRecords())
                    .flatMap(Collection::stream)
                    .forEach(record -> {
                        log.info("Event Name {} from region {} s3 bucket {} object key {}", record.getEventName(), record.getAwsRegion(), record.getS3().getBucket().getName(), record.getS3().getObject().getKey());
                    });

            return s3EventNotification;
        } catch (Exception ex) {
            log.warn("JSON parse/ mapping exception occurred. ", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public S3EventNotification deserialize(String s, byte[] bytes) {
        return deserialize(s, null, bytes);
    }
}