package com.bank.balance.service;

import com.bank.balance.dto.TransactionMessage;
import com.bank.balance.exception.AccountNotFoundException;
import com.bank.balance.model.Account;
import com.bank.balance.repository.AccountRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(accountRepository, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Deve salvar quando transacao for aprovada")
    void deveSalvarQuandoTransacaoAprovada() {
        balanceService.processBalanceUpdate(buildMessage("APPROVED", 1_000_000L, "100.00"));
        verify(accountRepository, times(1)).saveIfNewer(any());
    }

    @Test
    @DisplayName("Nao deve salvar quando transacao for rejeitada")
    void naoDeveSalvarQuandoTransacaoRejeitada() {
        balanceService.processBalanceUpdate(buildMessage("REJECTED", 1_000_000L, "100.00"));
        verify(accountRepository, never()).saveIfNewer(any());
    }

    @Test
    @DisplayName("Deve salvar conta com dados corretos do payload")
    void deveSalvarContaComDadosCorretos() {
        balanceService.processBalanceUpdate(buildMessage("APPROVED", 1_000_000L, "150.75"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).saveIfNewer(captor.capture());

        assertThat(captor.getValue().accountCode()).isEqualTo("acc-123");
        assertThat(captor.getValue().ownerCode()).isEqualTo("owner-456");
        assertThat(captor.getValue().balanceAmount()).isEqualByComparingTo("150.75");
        assertThat(captor.getValue().currencyCode()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("Nao deve salvar para qualquer status diferente de APPROVED")
    void naoDeveSalvarParaStatusDiferenteDeApproved() {
        for (String status : new String[]{"REJECTED", "PENDING", "CANCELLED"}) {
            balanceService.processBalanceUpdate(buildMessage(status, 1_000_000L, "100.00"));
        }
        verify(accountRepository, never()).saveIfNewer(any());
    }

    @Test
    @DisplayName("Deve salvar normalmente quando saldo for zero")
    void deveSalvarQuandoSaldoZero() {
        balanceService.processBalanceUpdate(buildMessage("APPROVED", 1_000_000L, "0.00"));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).saveIfNewer(captor.capture());

        assertThat(captor.getValue().balanceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve retornar saldo quando conta existe")
    void deveRetornarSaldoQuandoContaExiste() {
        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(buildAccount("acc-123", "100.00")));

        var response = balanceService.getBalance("acc-123");

        assertThat(response.id()).isEqualTo("acc-123");
        assertThat(response.owner()).isEqualTo("owner-456");
        assertThat(response.balance().amount()).isEqualByComparingTo("100.00");
        assertThat(response.balance().currency()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("Deve retornar updated_at com offset de Brasilia")
    void deveRetornarUpdatedAtComOffsetBrasilia() {
        // 13:00 UTC = 10:00 no horário de Brasília (-03:00)
        var instant = Instant.parse("2024-01-15T13:00:00Z");
        var conta = new Account("acc-123", "owner-456", new BigDecimal("100.00"), "BRL", instant, "ok");
        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(conta));

        var response = balanceService.getBalance("acc-123");

        assertThat(response.updatedAt().getOffset()).isEqualTo(ZoneOffset.of("-03:00"));
        assertThat(response.updatedAt().getHour()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve lancar AccountNotFoundException quando conta nao existe")
    void deveLancarExcecaoQuandoContaNaoExiste() {
        when(accountRepository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> balanceService.getBalance("nao-existe"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("nao-existe");
    }

    @Test
    @DisplayName("Deve propagar excecao quando repository falhar na consulta")
    void devePropagarExcecaoQuandoRepositoryFalharNaConsulta() {
        when(accountRepository.findById(any())).thenThrow(new RuntimeException("DynamoDB indisponivel"));

        assertThatThrownBy(() -> balanceService.getBalance("acc-123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DynamoDB indisponivel");
    }

    @Test
    @DisplayName("Deve propagar excecao quando repository falhar ao salvar")
    void devePropagarExcecaoQuandoRepositoryFalharAoSalvar() {
        doThrow(new RuntimeException("DynamoDB indisponivel")).when(accountRepository).saveIfNewer(any());

        assertThatThrownBy(() -> balanceService.processBalanceUpdate(buildMessage("APPROVED", 1_000_000L, "100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DynamoDB indisponivel");
    }

    private TransactionMessage buildMessage(String status, long timestampMicros, String balanceAmount) {
        return new TransactionMessage(
                new TransactionMessage.TransactionData("tx-001", "DEBIT", new BigDecimal("50.00"), "BRL", status, timestampMicros),
                new TransactionMessage.AccountData("acc-123", "owner-456", "123456", "ok",
                        new TransactionMessage.BalanceData(new BigDecimal(balanceAmount), "BRL"))
        );
    }

    private Account buildAccount(String accountId, String amount) {
        return new Account(accountId, "owner-456", new BigDecimal(amount), "BRL", Instant.now(), "ok");
    }
}