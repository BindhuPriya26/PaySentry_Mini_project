package com.paysentry.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.User;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByUser(User user);

    Optional<BankAccount> findByAccountId(String accountId);
}
