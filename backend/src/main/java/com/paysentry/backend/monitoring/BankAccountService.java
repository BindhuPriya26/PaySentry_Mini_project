package com.paysentry.backend.monitoring;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.repository.BankAccountRepository;

@Service
public class BankAccountService {

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("250000.00");

    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public BankAccount createForUser(User user) {
        return bankAccountRepository.findByUser(user).orElseGet(() -> {
            BankAccount account = new BankAccount();
            account.setUser(user);
            account.setAccountId(String.format("PS-%05d", 10000 + user.getId()));
            account.setTotalBalance(INITIAL_BALANCE);
            account.setAvailableBalance(INITIAL_BALANCE);
            account.setLienAmount(BigDecimal.ZERO);
            account.setCurrency("INR");
            return bankAccountRepository.save(account);
        });
    }

    @Transactional(readOnly = true)
    public BankAccount getForUser(User user) {
        return bankAccountRepository.findByUser(user)
            .orElseThrow(() -> new ResourceNotFoundException("No monitored banking account is linked to this user."));
    }

    @Transactional(readOnly = true)
    public BankAccount getByAccountId(String accountId) {
        return bankAccountRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Monitored account " + accountId + " was not found."));
    }
}
