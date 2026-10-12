package com.paysentry.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.GmailOauthState;
import com.paysentry.backend.entity.User;

public interface GmailOauthStateRepository extends JpaRepository<GmailOauthState, Long> {
    Optional<GmailOauthState> findByState(String state);
    long deleteByUser(User user);
    long deleteByExpiresAtBefore(LocalDateTime timestamp);
}
