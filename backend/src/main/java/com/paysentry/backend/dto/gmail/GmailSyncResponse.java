package com.paysentry.backend.dto.gmail;

public class GmailSyncResponse {
    private int messagesProcessed;
    private int alertsCreated;
    private String message;

    public int getMessagesProcessed() { return messagesProcessed; }
    public void setMessagesProcessed(int messagesProcessed) { this.messagesProcessed = messagesProcessed; }
    public int getAlertsCreated() { return alertsCreated; }
    public void setAlertsCreated(int alertsCreated) { this.alertsCreated = alertsCreated; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
