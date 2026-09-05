package com.paysentry.backend.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.dto.scan.UrlScanRequest;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.service.UrlScanService;

@RestController
@RequestMapping("/api/scans/url")
public class UrlScanController {

    private final UrlScanService urlScanService;

    public UrlScanController(UrlScanService urlScanService) {
        this.urlScanService = urlScanService;
    }

    @PostMapping
    public ResponseEntity<FraudScanResponse> scanUrl(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody UrlScanRequest request
    ) {
        return ResponseEntity.ok(urlScanService.scan(user, request));
    }
}
