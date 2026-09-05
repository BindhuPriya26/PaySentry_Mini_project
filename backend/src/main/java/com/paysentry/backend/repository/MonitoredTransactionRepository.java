package com.paysentry.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.MonitoredTransaction;

public interface MonitoredTransactionRepository extends JpaRepository<MonitoredTransaction, Long> {

    long countByAccountAndTransactionTimeAfter(BankAccount account, LocalDateTime startTime);

    List<MonitoredTransaction> findTop10ByAccountOrderByTransactionTimeDesc(BankAccount account);
}
