package com.paysentry.backend.dto.monitoring;

import javax.validation.constraints.NotBlank;

public class MobileDeviceRegistrationRequest {

    @NotBlank
    private String fcmToken;

    private String deviceName;

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
