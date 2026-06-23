package com.bank.balance.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMessageTest {

    private TransactionMessage.TransactionData buildTransaction(long timestampMicros) {
        return new TransactionMessage.TransactionData(
                "tx-001", "DEBIT", new BigDecimal("50.00"), "BRL", "APPROVED", timestampMicros
        );
    }

    @Test
    @DisplayName("Deve converter microssegundos para Instant corretamente")
    void deveConverterMicrossegundosParaInstant() {
        // 1_000_000 microssegundos = 1 segundo = epoch + 1s
        assertThat(buildTransaction(1_000_000L).transactionInstant())
                .isEqualTo(Instant.ofEpochSecond(1, 0));
    }

    @Test
    @DisplayName("Deve preservar precisao de microssegundos na conversao")
    void devePreservarPrecisaoDeMicrossegundos() {
        // 1_000_500 microssegundos = 1s + 500 micros = 1s + 500_000 nanos
        Instant resultado = buildTransaction(1_000_500L).transactionInstant();

        assertThat(resultado.getEpochSecond()).isEqualTo(1L);
        assertThat(resultado.getNano()).isEqualTo(500_000);
    }

    @Test
    @DisplayName("Deve retornar epoch quando timestamp for zero")
    void deveRetornarEpochParaTimestampZero() {
        assertThat(buildTransaction(0L).transactionInstant()).isEqualTo(Instant.EPOCH);
    }

    @Test
    @DisplayName("Deve converter o timestamp do enunciado sem erro")
    void deveConverterTimestampDoEnunciado() {
        // Timestamp do enunciado: 1751641364589998 microssegundos (julho/2025)
        Instant resultado = buildTransaction(1_751_641_364_589_998L).transactionInstant();

        assertThat(resultado).isNotNull();
        assertThat(resultado).isAfter(Instant.parse("2025-01-01T00:00:00Z"));
    }
}