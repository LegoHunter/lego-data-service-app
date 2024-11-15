package com.vattima.lego.data.service.kafka;

import com.vattima.lego.imaging.model.PhotoMetaData;
import com.vattima.lego.imaging.service.ImageManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class AwsS3EventNotificationListener {
    private final AtomicLong counter = new AtomicLong(0);
    private final S3Client s3Client;
    private final ImageManager imageManager;

    @KafkaListener(id = "aws-s3-event", topics = "aws-s3-event", groupId = "aws-s3-event-group", concurrency = "4", containerFactory = "kafkaJsonListenerContainerFactory")
    public void awsS3EventListener(S3EventNotification s3EventNotification) {

        log.info("Received aws-s3-event message {} : {}", counter.incrementAndGet(), s3EventNotification);

        Stream.ofNullable(s3EventNotification.getRecords())
                .flatMap(Collection::stream)
                .map(S3EventRecordHolder::new)
                .forEach(record -> {
                    S3ObjectHolder s3ObjectHolder = getObject(record.s3());
                    log.info("Event {} Bucket {} Object {} exists {} mime type {} isDirectory {} isImage {} isJPEG {}", record.eventName(), record.bucketName(), record.objectKey(), s3ObjectHolder.exists(), s3ObjectHolder.mimeType(), s3ObjectHolder.isDirectory(), s3ObjectHolder.isImage(), s3ObjectHolder.isJPeg());
                    switch (record.eventName()) {
                        case "ObjectCreated:Put" -> handleObjectCreated(s3ObjectHolder);
                        case "ObjectRemoved:Delete" -> handleObjectRemoved(s3ObjectHolder);
                    }
                });
    }

    private S3ObjectHolder getObject(S3 s3) {
        S3ObjectHolder s3ObjectHolder = new S3ObjectHolder(s3.getBucket().getName(), s3.getObject().getKey());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3.getBucket().getName())
                .key(s3.getObject().getKey())
                .build();

        try {
            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            s3ObjectHolder.responseInputStream(responseInputStream);
            s3ObjectHolder.exists(true);
        } catch (NoSuchKeyException e) {
            s3ObjectHolder.exists(false);
        } catch (AwsServiceException e) {
            throw new RuntimeException(e);
        } catch (SdkClientException e) {
            throw new RuntimeException(e);
        }
        return s3ObjectHolder;
    }

    private void handleObjectRemoved(final S3ObjectHolder s3ObjectHolder) {
        log.info("S3 Object bucket {} name {} removed", s3ObjectHolder.bucketName(), s3ObjectHolder.objectKey());
    }

    private void handleDirectoryCreated(final S3ObjectHolder s3ObjectHolder) {
        if (s3ObjectHolder.exists() & s3ObjectHolder.isDirectory()) {
            log.info("S3 Object bucket {} name {}", s3ObjectHolder.bucketName(), s3ObjectHolder.objectKey());
        }
    }

    private void handleObjectCreated(final S3ObjectHolder s3ObjectHolder) {
        if (s3ObjectHolder.exists() & s3ObjectHolder.isJPeg()) {
            InputStream stream = s3ObjectHolder.responseInputStream();

            PhotoMetaData photoMetaData = new PhotoMetaData(stream, Paths.get(s3ObjectHolder.bucketName()), Paths.get(s3ObjectHolder.objectKey()));
            imageManager.getKeywords(photoMetaData);
            String md5 = photoMetaData.getMd5();
            Map<String, String> keywords = photoMetaData.getKeywords();

            log.info("S3 Object bucket {} name {} md5 {} keywords {}", s3ObjectHolder.bucketName(), s3ObjectHolder.objectKey(), md5, keywords);
        }
    }

    @RequiredArgsConstructor
    private static class S3ObjectHolder {
        private final String bucketName;
        private final String objectKey;
        private MimeType mimeType;

        private ResponseInputStream<GetObjectResponse> responseInputStream;
        private boolean exists = false;

        public String bucketName() {
            return bucketName;
        }

        public String objectKey() {
            return objectKey;
        }

        public InputStream inputStream() throws IOException {
            return new ByteArrayInputStream(responseInputStream().readAllBytes());
        }

        public void responseInputStream(ResponseInputStream<GetObjectResponse> responseInputStream) {
            this.responseInputStream = responseInputStream;
        }

        public ResponseInputStream<GetObjectResponse> responseInputStream() {
            return responseInputStream;
        }

        public Optional<MimeType> mimeType() {
            if (exists()) {
                return Optional.ofNullable(mimeType)
                        .map(Optional::of)
                        .orElseGet(() -> {
                            this.mimeType = MimeTypeUtils.parseMimeType(responseInputStream().response().contentType());
                            return Optional.of(this.mimeType);
                        });
            } else {
                return Optional.empty();
            }
        }

        public boolean isDirectory() {
            return mimeType().map(mt -> mt.getSubtype().equals("x-directory")).orElse(false);
        }

        public boolean isImage() {
            return mimeType().map(mt -> mt.getType().equals("image")).orElse(false);
        }

        public boolean isJPeg() {
            return mimeType().map(MimeTypeUtils.IMAGE_JPEG::equals).orElse(false);
        }

        public void exists(boolean exists) {
            this.exists = exists;
        }

        public boolean exists() {
            return this.exists;
        }
    }

    @Data
    @RequiredArgsConstructor
    private static class S3EventRecordHolder {
        private final S3EventNotificationRecord record;

        public S3 s3() {
            return record.getS3();
        }

        public String eventName() {
            return record.getEventName();
        }

        public String bucketName() {
            return record.getS3().getBucket().getName();
        }

        public String objectKey() {
            return record.getS3().getObject().getKey();
        }
    }
}
