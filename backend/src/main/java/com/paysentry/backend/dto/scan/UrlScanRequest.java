package com.paysentry.backend.dto.scan;

import javax.validation.constraints.NotBlank;

public class UrlScanRequest {

    @NotBlank(message = "URL is required")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
