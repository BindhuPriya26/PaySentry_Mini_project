package com.paysentry.backend.dto.scan;

import javax.validation.constraints.NotBlank;

public class SmsScanRequest {

    @NotBlank(message = "SMS content is required")
    private String messageContent;

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
}
