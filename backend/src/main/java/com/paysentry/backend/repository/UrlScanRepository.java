package com.paysentry.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.UrlScan;
import com.paysentry.backend.entity.User;

public interface UrlScanRepository extends JpaRepository<UrlScan, Long> {

    long countByUser(User user);
}
