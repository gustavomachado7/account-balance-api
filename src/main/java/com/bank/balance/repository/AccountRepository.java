package com.bank.balance.repository;

import com.bank.balance.exception.RepositoryException;
import com.bank.balance.model.Account;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountRepository {

    private final DynamoDbTable<AccountItem> accountTable;
    private final MeterRegistry meterRegistry;


    public Optional<Account> findById(String accountId) {

        long start = System.currentTimeMillis();

        try {

            Key key = Key.builder()
                    .partitionValue(accountId)
                    .build();


            AccountItem item = accountTable.getItem(key);


            return Optional.ofNullable(item)
                    .map(this::toDomain);

        } catch (Exception ex) {

            log.error(
                    "Erro ao consultar saldo. accountId={} tempo={}ms",
                    accountId,
                    System.currentTimeMillis() - start,
                    ex
            );

            throw new RepositoryException(
                    "Erro ao consultar saldo da conta",
                    ex);
        }
    }

    // Exclusivo para ambiente de desenvolvimento
    // Scan não é recomendado para um cenário de produção com grande volume de dados
    public List<Account> findAccounts(int limit) {

        try {

            return accountTable.scan(
                            r -> r.limit(limit)
                    )
                    .items()
                    .stream()
                    .limit(limit)
                    .map(this::toDomain)
                    .toList();

        } catch (Exception ex) {

            log.error(
                    "Erro ao listar contas. limit={}",
                    limit,
                    ex
            );

            throw new RepositoryException(
                    "Erro ao listar contas",
                    ex
            );
        }
    }


    public void saveIfNewer(Account account) {

        long start = System.currentTimeMillis();

        try {

            Expression expression =
                    Expression.builder()
                            .expression(
                                    "attribute_not_exists(lastUpdatedAt) OR lastUpdatedAt < :newDate"
                            )
                            .putExpressionValue(
                                    ":newDate",
                                    AttributeValue.builder()
                                            .s(account.lastUpdatedAt().toString())
                                            .build()
                            )
                            .build();


            UpdateItemEnhancedRequest<AccountItem> request =
                    UpdateItemEnhancedRequest.builder(AccountItem.class)
                            .item(toItem(account))
                            .conditionExpression(expression)
                            .build();


            accountTable.updateItem(request);

        } catch (ConditionalCheckFailedException ex) {

            meterRegistry
                    .counter("balance.updates.ignored")
                    .increment();

            log.debug(
                    "Update ignorado. Mensagem mais antiga. accountId={}",
                    account.accountCode()
            );

        } catch (Exception ex) {

            log.error(
                    "Erro ao atualizar saldo. accountId={} tempo={}ms",
                    account.accountCode(),
                    System.currentTimeMillis() - start,
                    ex
            );

            throw new RepositoryException(
                    "Erro ao atualizar saldo da conta",
                    ex
            );
        }
    }


    private Account toDomain(AccountItem item) {

        return new Account(
                item.getAccountCode(),
                item.getOwnerCode(),
                item.getBalanceAmount(),
                item.getCurrencyCode(),
                item.getLastUpdatedAt(),
                item.getAccountStatus()
        );
    }


    private AccountItem toItem(Account account) {

        AccountItem item = new AccountItem();

        item.setAccountCode(account.accountCode());
        item.setOwnerCode(account.ownerCode());
        item.setBalanceAmount(account.balanceAmount());
        item.setCurrencyCode(account.currencyCode());
        item.setLastUpdatedAt(account.lastUpdatedAt());
        item.setAccountStatus(account.accountStatus());

        return item;
    }
}