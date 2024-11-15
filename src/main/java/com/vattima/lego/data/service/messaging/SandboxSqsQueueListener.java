package com.vattima.lego.data.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SandboxSqsQueueListener implements MessageListener {

    private final KafkaTemplate<String, String> awsS3EventKafkaTemplate;

    @Override
    public void onMessage(Message message) {
        try {
            // Message received from SQSClient.
            String sqsEventBody = ((TextMessage)message).getText();
            S3EventNotification s3EventNotification = S3EventNotification.fromJson(sqsEventBody);

            // Use getRecords() to access all the records in the notification.
            List<S3EventNotificationRecord> records = s3EventNotification.getRecords();

            S3EventNotificationRecord record = records.stream().findFirst().orElseThrow(() -> new RuntimeException("No S3 event notification records were present in the message"));
            // Use getters on the record to access individual attributes.
            String awsRegion = record.getAwsRegion();
            String eventName = record.getEventName();
            String eventSource = record.getEventSource();
            log.info("Received Sqs event {} from {} source {} with body {}", eventName, awsRegion, eventSource, sqsEventBody);

            log.info("sending S3EventNotification to Kafka {}", s3EventNotification);
            //awsS3EventKafkaTemplate.send("aws-s3-event", s3EventNotification);
            awsS3EventKafkaTemplate.send("aws-s3-event", sqsEventBody);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

//    private final ResourceLoader resourceLoader;
//
//    @MessageListener("sandbox-sqs-queue")
//    public void receiveRecordMessage(String event) throws IOException {
//        log.info("Received message: {}", event);
//
//        Resource resource = resourceLoader.getResource("");
//
//        try (InputStream inputStream = resource.getInputStream()) {
//            log.info("loading inputstream of s3 object [%s]");
//        }
//    }
}
