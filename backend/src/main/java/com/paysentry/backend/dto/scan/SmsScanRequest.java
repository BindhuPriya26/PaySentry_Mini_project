package com.paysentry.backend.dto.scan;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class SmsScanRequest {

    @NotBlank(message = "SMS content is required")
    @Size(max = 10000, message = "SMS content must be 10,000 characters or fewer")
    private String messageContent;

    private boolean consentConfirmed;

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public boolean isConsentConfirmed() {
        return consentConfirmed;
    }

    public void setConsentConfirmed(boolean consentConfirmed) {
        this.consentConfirmed = consentConfirmed;
    }
}
