package com.paysentry.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.SmsScan;
import com.paysentry.backend.entity.User;

public interface SmsScanRepository extends JpaRepository<SmsScan, Long> {

    long countByUser(User user);
}
