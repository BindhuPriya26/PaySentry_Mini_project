package com.paysentry.backend.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.dto.scan.SmsScanRequest;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.service.SmsScanService;

@RestController
@RequestMapping("/api/scans/sms")
public class SmsScanController {

    private final SmsScanService smsScanService;

    public SmsScanController(SmsScanService smsScanService) {
        this.smsScanService = smsScanService;
    }

    @PostMapping
    public ResponseEntity<FraudScanResponse> scanSms(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody SmsScanRequest request
    ) {
        return ResponseEntity.ok(smsScanService.scan(user, request));
    }
}
