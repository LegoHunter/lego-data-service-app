package com.vattima.lego.data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {"com.vattima"})
@RequiredArgsConstructor
@Slf4j
public class LegoDataServiceApplication {

    //private final KafkaTemplate<String, String> kafkaTemplate;

    private final RestTemplate awsRolesAnywhereRestTemplate;

    private final ObjectMapper objectMapper;

    public static void main(String[] args) {
        SpringApplication.run(LegoDataServiceApplication.class, args);
    }

//    @Bean
//    public CommandLineRunner sendEvents() {
//        return new CommandLineRunner() {
//            @Override
//            public void run(String... args) throws Exception {
//                for (int i = 0; i < 1000; i++) {
//                    kafkaTemplate.send("aws-chat-room", "Hello my love!");
//                }
//            }
//        };
//    }

}
