package com.paysentry.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.service.QrScanService;

@RestController
@RequestMapping("/api/scans/qr")
public class QrScanController {

    private final QrScanService qrScanService;

    public QrScanController(QrScanService qrScanService) {
        this.qrScanService = qrScanService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FraudScanResponse> scanQr(
        @AuthenticationPrincipal User user,
        @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(qrScanService.scan(user, file));
    }
}
