package com.bank.balance.repository;

import com.bank.balance.model.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;

@SpringBootTest
@Testcontainers
class AccountRepositoryTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.7.2"))
            .withServices(DYNAMODB, SQS);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("aws.endpoint", () -> localStack.getEndpoint().toString());
        registry.add("aws.region", () -> localStack.getRegion());
        registry.add("aws.access-key", localStack::getAccessKey);
        registry.add("aws.secret-key", localStack::getSecretKey);
        registry.add("aws.sqs.queue-name", () -> "test-queue");
        registry.add("aws.dynamodb.table-name", () -> "test-accounts");
    }

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Deve salvar e recuperar conta corretamente")
    void deveSalvarERecuperarConta() {
        accountRepository.saveIfNewer(buildAccount("acc-salvar-recuperar", "100.00", Instant.parse("2024-01-15T10:00:00Z")));

        Optional<Account> resultado = accountRepository.findById("acc-salvar-recuperar");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().balanceAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Deve retornar vazio para conta inexistente")
    void deveRetornarVazioParaContaInexistente() {
        assertThat(accountRepository.findById("nao-existe-xyz")).isEmpty();
    }

    @Test
    @DisplayName("Deve atualizar saldo quando nova mensagem for mais recente")
    void deveAtualizarSaldoQuandoMensagemMaisRecente() {
        accountRepository.saveIfNewer(buildAccount("acc-atualizar-novo", "100.00", Instant.parse("2024-01-15T10:00:00Z")));
        accountRepository.saveIfNewer(buildAccount("acc-atualizar-novo", "200.00", Instant.parse("2024-01-15T11:00:00Z")));

        assertThat(accountRepository.findById("acc-atualizar-novo").get().balanceAmount())
                .isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Nao deve atualizar saldo quando nova mensagem for mais antiga")
    void naoDeveAtualizarSaldoQuandoMensagemMaisAntiga() {
        accountRepository.saveIfNewer(buildAccount("acc-ignorar-antigo", "100.00", Instant.parse("2024-01-15T10:00:00Z")));
        accountRepository.saveIfNewer(buildAccount("acc-ignorar-antigo", "50.00", Instant.parse("2024-01-15T09:00:00Z")));

        assertThat(accountRepository.findById("acc-ignorar-antigo").get().balanceAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Nao deve atualizar saldo quando timestamp for igual")
    void naoDeveAtualizarSaldoQuandoTimestampIgual() {
        var timestamp = Instant.parse("2024-01-15T10:00:00Z");
        accountRepository.saveIfNewer(buildAccount("acc-mesmo-timestamp", "100.00", timestamp));
        accountRepository.saveIfNewer(buildAccount("acc-mesmo-timestamp", "999.00", timestamp));

        assertThat(accountRepository.findById("acc-mesmo-timestamp").get().balanceAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Deve salvar normalmente quando saldo for zero")
    void deveSalvarQuandoSaldoZero() {
        accountRepository.saveIfNewer(buildAccount("acc-saldo-zero", "0.00", Instant.now()));

        Optional<Account> resultado = accountRepository.findById("acc-saldo-zero");
        assertThat(resultado).isPresent();
        assertThat(resultado.get().balanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Account buildAccount(String accountId, String amount, Instant timestamp) {
        return new Account(accountId, "owner-teste", new BigDecimal(amount), "BRL", timestamp, "ok");
    }
}