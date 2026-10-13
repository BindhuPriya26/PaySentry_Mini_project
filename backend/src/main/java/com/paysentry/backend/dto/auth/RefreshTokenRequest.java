package com.paysentry.backend.dto.auth;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Size(max = 128, message = "Refresh token is invalid")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
