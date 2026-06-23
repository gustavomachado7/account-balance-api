package com.bank.balance.repository;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;
import java.time.Instant;

// Não pode ser record porque o DynamoDB Enhanced Client exige getters/setters para serialização
@Getter
@Setter
@DynamoDbBean
public class AccountItem {

    private String accountCode;
    private String ownerCode;
    private BigDecimal balanceAmount;
    private String currencyCode;
    private Instant lastUpdatedAt;
    private String accountStatus;

    @DynamoDbPartitionKey
    public String getAccountCode() {
        return accountCode;
    }
}