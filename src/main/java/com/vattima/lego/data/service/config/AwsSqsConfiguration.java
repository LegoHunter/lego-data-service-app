package com.vattima.lego.data.service.config;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vattima.lego.data.service.messaging.SandboxSqsQueueListener;
import crypto.rsa.RSA;
import lego.aws.configuration.model.AwsIamRolesAnywhereProperties;
import lego.aws.configuration.model.AwsRolesAnywhereProperties;
import lego.aws.iam.rolesanywhere.model.AwsRolesAnywhereAuthRequest;
import lego.aws.iam.rolesanywhere.model.AwsRolesAnywhereAuthResponse;
import lego.aws.iam.rolesanywhere.model.CredentialSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;

@Configuration
@Slf4j
@EnableConfigurationProperties({AwsIamRolesAnywhereProperties.class})
public class AwsSqsConfiguration {

    @Bean
    public SQSConnectionFactory sqsConnectionFactory(SqsClient sqsClientt) {
        SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(), sqsClientt);
        return connectionFactory;
    }

    @Bean
    public RolesAnywhereCredentialsProvider rolesAnywhereCredentialsProvider(RestTemplate awsRolesAnywhereRestTemplate, AwsIamRolesAnywhereProperties awsIamRolesAnywhereProperties, ObjectMapper objectMapper) {
        return new RolesAnywhereCredentialsProvider(awsRolesAnywhereRestTemplate, awsIamRolesAnywhereProperties, objectMapper);
    }

    @Bean
    SqsAsyncClient sqsAsyncClient(RolesAnywhereCredentialsProvider rolesAnywhereCredentialsProvider) {
        SqsAsyncClient sqsAsyncClient = SqsAsyncClient
                .builder()
                .region(Region.of("us-east-1"))
                .credentialsProvider(rolesAnywhereCredentialsProvider)
                .build();

        return sqsAsyncClient;
        // add more Options
    }

    @Bean
    SqsClient sqsClient(RestTemplate awsRolesAnywhereRestTemplate, AwsIamRolesAnywhereProperties awsIamRolesAnywhereProperties, ObjectMapper objectMapper) {
        AwsCredentialsProvider awsCredentialsProvider = new RolesAnywhereCredentialsProvider(awsRolesAnywhereRestTemplate, awsIamRolesAnywhereProperties, objectMapper);

        SqsClient sqsClient = SqsClient
                .builder()
                .region(Region.of("us-east-1"))
                .credentialsProvider(awsCredentialsProvider)
                .build();

        return sqsClient;
        // add more Options
    }


    @Bean
    public MessageListenerContainer defaultMessageListenerContainer(SQSConnectionFactory sqsConnectionFactory, SandboxSqsQueueListener sandboxSqsQueueListener) {
        DefaultMessageListenerContainer defaultMessageListenerContainer = new DefaultMessageListenerContainer();
        defaultMessageListenerContainer.setConnectionFactory(sqsConnectionFactory);
        defaultMessageListenerContainer.setMessageListener(sandboxSqsQueueListener);
        defaultMessageListenerContainer.setDestinationName("sandbox-sqs-queue");
        return defaultMessageListenerContainer;
    }

    @Bean
    public RestTemplate awsRolesAnywhereRestTemplate(RestTemplateBuilder restTemplateBuilder, BeansEndpoint beansEndpoint) {
        return restTemplateBuilder.build();
    }

    @RequiredArgsConstructor
    @Slf4j
    public static class RolesAnywhereCredentialsProvider implements AwsCredentialsProvider {
        private final RestTemplate awsRolesAnywhereRestTemplate;
        private final AwsIamRolesAnywhereProperties awsIamRolesAnywhereProperties;
        private final ObjectMapper objectMapper;

        @Override
        public AwsCredentials resolveCredentials() {
            log.info("Refreshing RolesAnywhere credentials");

            final String METHOD = "POST";
            final String SERVICE = "rolesanywhere";
            final String REGION = "us-east-1";

            final String RESTAPIHOST = "%s.%s.amazonaws.com".formatted(SERVICE, REGION);
            final String RESTAPIPATH = "/sessions";

            final String ALGORITHM = "AWS4-X509-RSA-SHA256";
            final String X_AMZ_DATE_HEADER_NAME = "X-Amz-Date";
            final String X_AMZ_X509_HEADER_NAME = "X-Amz-X509";

            final String AUTHORIZATION_HEADER_TEMPLATE = "%s Credential=%d/%s, SignedHeaders=%s, Signature=%s";
            final String CANONICAL_REQUEST_TEMPLATE = "%s\n%s\n%s\n%s\n%s\n%s";
            final String CANONICAL_HEADERS_TEMPLATE = "content-type:%s\nhost:%s\n%s:%s\n%s:%s\n";
            final String CREDENTIAL_SCOPE_TEMPLATE = "%s/%s/%s/aws4_request";
            final String SIGNED_HEADERS = "content-type;host;%s;%s".formatted(X_AMZ_DATE_HEADER_NAME.toLowerCase(), X_AMZ_X509_HEADER_NAME.toLowerCase());
            final String SIGNING_TEMPLATE = "%s\n%s\n%s\n%s";

            // Get the private key and the certificate
            AwsRolesAnywhereAuthResponse rolesAnywhereResponse = null;
            try {
                PrivateKey privateKey = RSA.getPrivatePemKey(new ClassPathResource("clientEntity.key").getInputStream());
                X509Certificate x509Certificate = (X509Certificate) RSA.getCertificate(new ClassPathResource("clientEntity.pem").getInputStream());
                String certificate = RSA.encode(x509Certificate.getEncoded());

                // Create a datetime object for signing
                ZonedDateTime now = ZonedDateTime.now();
                DateTimeFormatter longDateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.US).withZone(UTC);
                DateTimeFormatter shortDateTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US).withZone(UTC);
                String dateTimeStampNow = longDateTimeFormat.format(now);
                String dateStampNow = shortDateTimeFormat.format(now);

                // Create the canonical request
                String canonicalUri = RESTAPIPATH;
                String canonicalQuerystring = "";

                String canonicalHeaders = CANONICAL_HEADERS_TEMPLATE.formatted(MediaType.APPLICATION_JSON, RESTAPIHOST, X_AMZ_DATE_HEADER_NAME.toLowerCase(), dateTimeStampNow, X_AMZ_X509_HEADER_NAME.toLowerCase(), certificate);

                String iamRolesAnywhereProfileName = "legolandserver";
                AwsRolesAnywhereProperties awsRolesAnywhereProperties = awsIamRolesAnywhereProperties.getAwsIamRolesAnywhereMapProperties()
                        .rolesAnywhereProfile(iamRolesAnywhereProfileName)
                        .orElseThrow(() -> new RuntimeException("Unable to find rolesAnywhere profile name [%s]".formatted(iamRolesAnywhereProfileName)));

                // Request parameters for CreateSession--passed in a JSON block.
                AwsRolesAnywhereAuthRequest awsRolesAnywhereAuthRequest = AwsRolesAnywhereAuthRequest.builder()
                        .durationSeconds(awsRolesAnywhereProperties.getDurationSeconds())
                        .profileArn(awsRolesAnywhereProperties.getProfileArn())
                        .roleArn(awsRolesAnywhereProperties.getRoleArn())
                        .trustAnchorArn(awsRolesAnywhereProperties.getTrustAnchorArn())
                        .build();
                String payload = objectMapper.writeValueAsString(awsRolesAnywhereAuthRequest);
                String payloadHash = RSA.sha256Hex(payload);

                String canonicalRequest = CANONICAL_REQUEST_TEMPLATE.formatted(METHOD, canonicalUri, canonicalQuerystring, canonicalHeaders, SIGNED_HEADERS, payloadHash);

                // Create the string to sign
                String credentialScope = CREDENTIAL_SCOPE_TEMPLATE.formatted(dateStampNow, REGION, SERVICE);
                String hashedCanonicalRequest = RSA.sha256Hex(canonicalRequest);
                String stringToSign = SIGNING_TEMPLATE.formatted(ALGORITHM, dateTimeStampNow, credentialScope, hashedCanonicalRequest);

                // Sign the string
                String signature = RSA.bytesToHex(RSA.sign(privateKey, stringToSign, "SHA256withRSA"));

                // Add signing information to the request
                String authorizationHeader = AUTHORIZATION_HEADER_TEMPLATE.formatted(ALGORITHM, x509Certificate.getSerialNumber(), credentialScope, SIGNED_HEADERS, signature);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("Accept-Encoding", "gzip");
                headers.add("Host", RESTAPIHOST);
                headers.add("Authorization", authorizationHeader);
                headers.add(X_AMZ_DATE_HEADER_NAME, dateTimeStampNow);
                headers.add(X_AMZ_X509_HEADER_NAME, certificate);

                HttpEntity<String> request = new HttpEntity<>(payload, headers);
                String url = "https://%s/sessions".formatted(RESTAPIHOST);
                ResponseEntity<AwsRolesAnywhereAuthResponse> response = awsRolesAnywhereRestTemplate.exchange(url, HttpMethod.POST, request, AwsRolesAnywhereAuthResponse.class);

                rolesAnywhereResponse = response.getBody();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return Stream.ofNullable(rolesAnywhereResponse.getCredentialSet())
                    .flatMap(Collection::stream)
                    .findFirst()
                    .map(CredentialSet::getCredentials)
                    .map(credentials ->
                            new AwsSessionCredentials.Builder()
                                    .accessKeyId(credentials.getAccessKeyId())
                                    .expirationTime(credentials.getExpiration().toInstant())
                                    .secretAccessKey(credentials.getSecretAccessKey())
                                    .sessionToken(credentials.getSessionToken())
                                    .build())
                    .orElseThrow(() -> new RuntimeException("No CredentialSet present in RolesAnywhere response"));
        }

    }
}
