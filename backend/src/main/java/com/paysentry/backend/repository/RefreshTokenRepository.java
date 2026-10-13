package com.paysentry.backend.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paysentry.backend.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshToken token where token.tokenHash = :tokenHash "
        + "and token.expiresAt > :now")
    Optional<RefreshToken> findActiveForUpdate(
        @Param("tokenHash") String tokenHash,
        @Param("now") LocalDateTime now
    );

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long deleteByExpiresAtBefore(LocalDateTime now);

    boolean existsBySessionIdAndExpiresAtAfter(String sessionId, LocalDateTime now);
}
