package com.paysentry.backend.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.detection.ContentDetectionRequest;
import com.paysentry.backend.dto.detection.ContentDetectionResponse;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.monitoring.MobileDetectionService;

@RestController
@RequestMapping("/api/detections")
public class MobileDetectionController {

    private final MobileDetectionService mobileDetectionService;

    public MobileDetectionController(MobileDetectionService mobileDetectionService) {
        this.mobileDetectionService = mobileDetectionService;
    }

    @PostMapping("/sms")
    public ResponseEntity<ContentDetectionResponse> scanSms(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ContentDetectionRequest request
    ) {
        if (!request.isConsentConfirmed()) {
            throw new BadRequestException("Confirm that you want to send this SMS to PaySentry for analysis.");
        }
        return ResponseEntity.ok(mobileDetectionService.analyze(user, BankingEventType.SMS, request.getContent()));
    }

    @PostMapping("/url")
    public ResponseEntity<ContentDetectionResponse> scanUrl(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ContentDetectionRequest request
    ) {
        return ResponseEntity.ok(mobileDetectionService.analyze(user, BankingEventType.URL, request.getContent()));
    }

    @PostMapping("/qr")
    public ResponseEntity<ContentDetectionResponse> scanQrContent(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody ContentDetectionRequest request
    ) {
        return ResponseEntity.ok(mobileDetectionService.analyze(user, BankingEventType.QR, request.getContent()));
    }
}
