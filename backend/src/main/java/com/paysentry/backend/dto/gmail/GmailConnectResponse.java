package com.paysentry.backend.dto.gmail;

public class GmailConnectResponse {
    private String authorizationUrl;
    private String message;

    public String getAuthorizationUrl() { return authorizationUrl; }
    public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
