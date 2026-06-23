package com.bank.balance.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Account(

        String accountCode,

        String ownerCode,

        // Utilizei BigDecimal para evitar erros de arredondamento em valores monetários
        BigDecimal balanceAmount,

        String currencyCode,

        Instant lastUpdatedAt,

        String accountStatus
) {
}