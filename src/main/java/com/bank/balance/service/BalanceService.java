package com.bank.balance.service;

import com.bank.balance.dto.AccountSampleResponse;
import com.bank.balance.dto.BalanceResponse;
import com.bank.balance.dto.TransactionMessage;
import com.bank.balance.exception.AccountNotFoundException;
import com.bank.balance.model.Account;
import com.bank.balance.repository.AccountRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private static final String APPROVED_STATUS = "APPROVED";

    private final AccountRepository accountRepository;
    private final MeterRegistry meterRegistry;

    public void processBalanceUpdate(TransactionMessage message) {

        var transaction = message.transaction();
        var accountData = message.account();

        if (!APPROVED_STATUS.equals(transaction.status())) {

            meterRegistry
                    .counter("transactions.ignored")
                    .increment();

            log.debug(
                    "Transacao ignorada. transactionId={} status={}",
                    transaction.id(),
                    transaction.status()
            );
            return;
        }

        Account account = new Account(
                accountData.id(),
                accountData.owner(),
                accountData.balance().amount(),
                accountData.balance().currency(),
                transaction.transactionInstant(),
                accountData.status()
        );

        accountRepository.saveIfNewer(account);

    }

    public BalanceResponse getBalance(String accountId) {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new AccountNotFoundException(accountId));

        OffsetDateTime updatedAt =
                OffsetDateTime.ofInstant(
                        account.lastUpdatedAt(),
                        ZoneOffset.of("-03:00"));

        return new BalanceResponse(
                account.accountCode(),
                account.ownerCode(),
                new BalanceResponse.BalanceData(
                        account.balanceAmount(),
                        account.currencyCode()
                ),
                updatedAt
        );
    }


    public List<AccountSampleResponse> getAccountIds(int limit) {

        int safeLimit = Math.min(limit, 50);

        return accountRepository.findAccounts(safeLimit)
                .stream()
                .map(account ->
                        new AccountSampleResponse(
                                account.accountCode()
                        )
                )
                .toList();
    }
}