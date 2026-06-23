package com.bank.balance.consumer;

import com.bank.balance.dto.TransactionMessage;
import com.bank.balance.service.BalanceService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionConsumer {

    private final BalanceService balanceService;
    private final MeterRegistry meterRegistry;

    // Nesse caso, iniciei os counters para aparecerem
    // nas métricas mesmo sem eventos
    @PostConstruct
    public void initMetrics() {
        meterRegistry.counter("sqs.messages.processed");
        meterRegistry.counter("sqs.messages.failed");
    }

    @Retryable(
            retryFor = {
                    TimeoutException.class,
                    DynamoDbException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2)
    )
    @SqsListener(
            value = "${aws.sqs.queue-name}",
            factory = "defaultSqsListenerContainerFactory"
    )
    public void receive(TransactionMessage message) {

        if (message == null
                || message.transaction() == null
                || message.account() == null) {

            log.warn("Mensagem invalida recebida. Ignorando.");
            return;
        }

        balanceService.processBalanceUpdate(message);

        meterRegistry
                .counter("sqs.messages.processed")
                .increment();
    }

    @Recover
    public void recover(Exception ex, TransactionMessage message) {

        meterRegistry
                .counter("sqs.messages.failed")
                .increment();

        log.error(
                "Todas as tentativas falharam. transactionId={} accountId={}",
                message.transaction().id(),
                message.account().id(),
                ex
        );

        throw new RuntimeException(ex);
    }
}