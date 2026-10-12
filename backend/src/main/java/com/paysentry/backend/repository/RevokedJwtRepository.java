package com.paysentry.backend.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.RevokedJwt;

public interface RevokedJwtRepository extends JpaRepository<RevokedJwt, String> {

    long deleteByExpiresAtBefore(LocalDateTime timestamp);
}
