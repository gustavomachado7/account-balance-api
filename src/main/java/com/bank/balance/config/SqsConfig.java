package com.bank.balance.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.net.URI;

@Configuration
public class SqsConfig {

    private static final int MAX_CONCURRENT_MESSAGES = 10;
    private static final int MAX_MESSAGES_PER_POLL = 10;

    private final String awsEndpoint;
    private final String awsRegion;
    private final String accessKey;
    private final String secretKey;


    public SqsConfig(
            @Value("${aws.endpoint}") String awsEndpoint,
            @Value("${aws.region}") String awsRegion,
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey
    ) {
        this.awsEndpoint = awsEndpoint;
        this.awsRegion = awsRegion;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }


    @Bean
    public SqsAsyncClient sqsAsyncClient() {

        return SqsAsyncClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(awsRegion))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey
                                )
                        )
                )
                .build();
    }


    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
            SqsAsyncClient sqsAsyncClient) {

        return SqsMessageListenerContainerFactory.builder()
                .configure(options -> options
                        .maxConcurrentMessages(MAX_CONCURRENT_MESSAGES)
                        .maxMessagesPerPoll(MAX_MESSAGES_PER_POLL)
                )
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}