package com.paysentry.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.GmailConnection;
import com.paysentry.backend.entity.GmailConnectionStatus;
import com.paysentry.backend.entity.User;

public interface GmailConnectionRepository extends JpaRepository<GmailConnection, Long> {
    Optional<GmailConnection> findByUser(User user);
    List<GmailConnection> findByStatus(GmailConnectionStatus status);
}
