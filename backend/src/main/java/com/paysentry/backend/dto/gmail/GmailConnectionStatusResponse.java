package com.paysentry.backend.dto.gmail;

import java.time.LocalDateTime;

public class GmailConnectionStatusResponse {
    private boolean configured;
    private boolean connected;
    private String mailboxEmail;
    private String status;
    private LocalDateTime lastSyncAt;
    private String message;

    public boolean isConfigured() { return configured; }
    public void setConfigured(boolean configured) { this.configured = configured; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
    public String getMailboxEmail() { return mailboxEmail; }
    public void setMailboxEmail(String mailboxEmail) { this.mailboxEmail = mailboxEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
