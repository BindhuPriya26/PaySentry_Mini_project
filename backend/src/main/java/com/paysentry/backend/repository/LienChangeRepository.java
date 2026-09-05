package com.paysentry.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.LienChange;

public interface LienChangeRepository extends JpaRepository<LienChange, Long> {

    long countByAccountAndChangedAtAfter(BankAccount account, LocalDateTime startTime);

    List<LienChange> findTop10ByAccountOrderByChangedAtDesc(BankAccount account);
}
