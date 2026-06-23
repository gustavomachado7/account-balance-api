package com.bank.balance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

// Uso de @EnableRetry para habilitar o
// suporte a retry com @Retryable nos beans do Spring
@EnableRetry
@SpringBootApplication
public class BalanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BalanceApplication.class, args);
    }
}