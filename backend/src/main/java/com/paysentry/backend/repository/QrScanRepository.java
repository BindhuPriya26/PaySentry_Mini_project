package com.paysentry.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.QrScan;
import com.paysentry.backend.entity.User;

public interface QrScanRepository extends JpaRepository<QrScan, Long> {

    long countByUser(User user);
}
