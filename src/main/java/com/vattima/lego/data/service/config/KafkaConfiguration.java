package com.vattima.lego.data.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@Slf4j
public class KafkaConfiguration {

    private final AtomicLong counter = new AtomicLong(0);

    @KafkaListener(id = "chat-room", topics = "chat-room", groupId = "${groupId:chat-room-group-1}", concurrency = "4")
    public void listen(String in) {
        log.info("Received message {} : {}", counter.incrementAndGet(), in);
    }
}
