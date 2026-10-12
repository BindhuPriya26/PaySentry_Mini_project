package com.paysentry.backend.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "revoked_jwts")
public class RevokedJwt {

    @Id
    @Column(name = "jwt_id", nullable = false, length = 64)
    private String jwtId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public String getJwtId() {
        return jwtId;
    }

    public void setJwtId(String jwtId) {
        this.jwtId = jwtId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
