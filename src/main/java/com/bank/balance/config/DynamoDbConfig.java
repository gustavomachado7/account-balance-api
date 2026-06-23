package com.bank.balance.config;

import com.bank.balance.repository.AccountItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Duration;

@Slf4j
@Configuration
public class DynamoDbConfig {

    private final String awsEndpoint;
    private final String awsRegion;
    private final String accessKey;
    private final String secretKey;
    private final String tableName;

    public DynamoDbConfig(
            @Value("${aws.endpoint}") String awsEndpoint,
            @Value("${aws.region}") String awsRegion,
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey,
            @Value("${aws.dynamodb.table-name}") String tableName
    ) {
        this.awsEndpoint = awsEndpoint;
        this.awsRegion = awsRegion;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.tableName = tableName;
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {

        return DynamoDbClient.builder()
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
                .overrideConfiguration(config -> config
                        .apiCallTimeout(Duration.ofSeconds(5))
                        .apiCallAttemptTimeout(Duration.ofSeconds(3))
                )
                // Pool de conexões com Apache HTTP Client para reutilizar conexões
                // e evitar overhead de abertura a cada requisição
                .httpClientBuilder(
                        ApacheHttpClient.builder()
                                .connectionTimeout(Duration.ofSeconds(2))
                                .socketTimeout(Duration.ofSeconds(2))
                                .maxConnections(100)
                                .connectionTimeToLive(Duration.ofSeconds(20))
                                .connectionMaxIdleTime(Duration.ofSeconds(5))
                )
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(
            DynamoDbClient dynamoDbClient) {

        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<AccountItem> accountTable(
            DynamoDbEnhancedClient enhancedClient) {

        DynamoDbTable<AccountItem> table =
                enhancedClient.table(
                        tableName,
                        TableSchema.fromBean(AccountItem.class)
                );

        try {
            table.createTable();
            log.info("Tabela DynamoDB criada. tableName={}", tableName);
        } catch (Exception ex) {
            log.info("Tabela ja existente. tableName={}", tableName);
        }

        return table;
    }
}