package com.paysentry.backend.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "gmail_connections")
public class GmailConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "mailbox_email", nullable = false, length = 320)
    private String mailboxEmail;

    @Lob
    @Column(name = "encrypted_access_token", nullable = false)
    private String encryptedAccessToken;

    @Lob
    @Column(name = "encrypted_refresh_token", nullable = false)
    private String encryptedRefreshToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    @Column(nullable = false, length = 600)
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GmailConnectionStatus status = GmailConnectionStatus.CONNECTED;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_error_at")
    private LocalDateTime lastErrorAt;

    @Column(name = "last_error_code", length = 60)
    private String lastErrorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getMailboxEmail() { return mailboxEmail; }
    public void setMailboxEmail(String mailboxEmail) { this.mailboxEmail = mailboxEmail; }
    public String getEncryptedAccessToken() { return encryptedAccessToken; }
    public void setEncryptedAccessToken(String encryptedAccessToken) { this.encryptedAccessToken = encryptedAccessToken; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public void setEncryptedRefreshToken(String encryptedRefreshToken) { this.encryptedRefreshToken = encryptedRefreshToken; }
    public LocalDateTime getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public void setAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) { this.accessTokenExpiresAt = accessTokenExpiresAt; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public GmailConnectionStatus getStatus() { return status; }
    public void setStatus(GmailConnectionStatus status) { this.status = status; }
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public LocalDateTime getLastErrorAt() { return lastErrorAt; }
    public void setLastErrorAt(LocalDateTime lastErrorAt) { this.lastErrorAt = lastErrorAt; }
    public String getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
