package com.paysentry.backend.security;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.entity.RevokedJwt;
import com.paysentry.backend.repository.RevokedJwtRepository;

@Service
public class JwtRevocationService {

    private final RevokedJwtRepository revokedJwtRepository;

    public JwtRevocationService(RevokedJwtRepository revokedJwtRepository) {
        this.revokedJwtRepository = revokedJwtRepository;
    }

    @Transactional
    public void revoke(String jwtId, Date expiry) {
        if (jwtId == null || jwtId.isBlank() || expiry == null || expiry.before(new Date())) {
            return;
        }
        RevokedJwt revokedJwt = new RevokedJwt();
        revokedJwt.setJwtId(jwtId);
        revokedJwt.setExpiresAt(LocalDateTime.ofInstant(expiry.toInstant(), ZoneId.systemDefault()));
        revokedJwtRepository.save(revokedJwt);
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jwtId) {
        return jwtId != null && revokedJwtRepository.existsById(jwtId);
    }

    @Transactional
    public void removeExpired() {
        revokedJwtRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
