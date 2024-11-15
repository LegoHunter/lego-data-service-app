package com.vattima.lego.data.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class AwsS3Configuration {

    @Bean
    public S3Client s3Client(AwsSqsConfiguration.RolesAnywhereCredentialsProvider rolesAnywhereCredentialsProvider) {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(rolesAnywhereCredentialsProvider)
                .build();
    }
}
