package com.bank.balance.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Conta nao encontrada: " + accountId);
    }
}