package com.tutorial.redis.module01.adapter.inbound.rest;

import com.tutorial.redis.module01.application.dto.AccountResponse;
import com.tutorial.redis.module01.domain.port.inbound.GetAccountBalanceUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final GetAccountBalanceUseCase getAccountBalanceUseCase;

    public AccountController(GetAccountBalanceUseCase getAccountBalanceUseCase) {
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountId) {
        return getAccountBalanceUseCase.getAccount(accountId)
                .map(AccountResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
