package com.paysentry.backend.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.TransactionRecord;
import com.paysentry.backend.entity.User;

public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, Long> {

    long countByUser(User user);

    long countByUserAndTransactionTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime);
}
