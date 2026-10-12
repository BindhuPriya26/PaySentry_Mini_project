package com.paysentry.backend.dto.scan;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UrlScanRequest {

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must be 2,048 characters or fewer")
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
