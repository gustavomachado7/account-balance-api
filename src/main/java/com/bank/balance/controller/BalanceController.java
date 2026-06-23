package com.bank.balance.controller;

import com.bank.balance.dto.AccountSampleResponse;
import com.bank.balance.dto.BalanceResponse;
import com.bank.balance.service.BalanceService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;


    @GetMapping("/")
    public ResponseEntity<?> home() {
        return ResponseEntity.ok(Map.of(
                "mensagem", "API de consulta de saldo em execucao",
                "documentacao", "Utilize /actuator/health para verificar a saude da aplicacao"
        ));
    }


    @GetMapping("/sample-accounts")
    public ResponseEntity<List<AccountSampleResponse>> getAccountIds(
            @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(
                balanceService.getAccountIds(limit)
        );
    }


    @GetMapping("/balances/{accountId}")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable @NotBlank String accountId) {

        log.info("Consulta de saldo recebida. accountId={}", accountId);

        return ResponseEntity.ok(balanceService.getBalance(accountId));
    }
}