package com.paysentry.backend.dto.detection;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ContentDetectionRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must be 10,000 characters or fewer")
    private String content;

    private boolean consentConfirmed;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isConsentConfirmed() {
        return consentConfirmed;
    }

    public void setConsentConfirmed(boolean consentConfirmed) {
        this.consentConfirmed = consentConfirmed;
    }
}
