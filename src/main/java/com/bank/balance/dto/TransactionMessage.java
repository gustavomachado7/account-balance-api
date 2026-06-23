package com.bank.balance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionMessage(

        TransactionData transaction,
        AccountData account

) {

    public record TransactionData(
            String id,
            String type,
            BigDecimal amount,
            String currency,
            String status,
            long timestamp
    ) {

        // Timestamp vem em microssegundos, então
        // converto para Instant para preservar a precisão
        public Instant transactionInstant() {

            long seconds = timestamp / 1_000_000;
            long micros = timestamp % 1_000_000;

            return Instant.ofEpochSecond(
                    seconds,
                    micros * 1_000
            );
        }
    }

    public record AccountData(
            String id,
            String owner,

            @JsonProperty("created_at")
            String createdAt,

            String status,

            BalanceData balance
    ) {
    }

    public record BalanceData(
            BigDecimal amount,
            String currency
    ) {
    }
}