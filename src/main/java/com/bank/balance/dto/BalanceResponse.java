package com.bank.balance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BalanceResponse(

        String id,
        String owner,
        BalanceData balance,

        @JsonProperty("updated_at")
        OffsetDateTime updatedAt

) {

    public record BalanceData(
            BigDecimal amount,
            String currency
    ) {
    }
}