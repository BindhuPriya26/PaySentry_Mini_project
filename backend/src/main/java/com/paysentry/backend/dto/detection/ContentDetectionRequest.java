package com.paysentry.backend.dto.detection;

import javax.validation.constraints.NotBlank;

public class ContentDetectionRequest {

    @NotBlank
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
