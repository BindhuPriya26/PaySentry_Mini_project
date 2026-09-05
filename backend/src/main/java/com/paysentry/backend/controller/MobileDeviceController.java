package com.paysentry.backend.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.monitoring.MobileDeviceRegistrationRequest;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.monitoring.MobileDeviceService;

@RestController
@RequestMapping("/api/mobile/devices")
public class MobileDeviceController {

    private final MobileDeviceService mobileDeviceService;

    public MobileDeviceController(MobileDeviceService mobileDeviceService) {
        this.mobileDeviceService = mobileDeviceService;
    }

    @PostMapping
    public ResponseEntity<Void> registerDevice(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody MobileDeviceRegistrationRequest request
    ) {
        mobileDeviceService.register(user, request.getFcmToken(), request.getDeviceName());
        return ResponseEntity.noContent().build();
    }
}
