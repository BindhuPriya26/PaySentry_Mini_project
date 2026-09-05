package com.paysentry.backend.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.scan.TransactionRiskRequest;
import com.paysentry.backend.dto.scan.TransactionRiskResponse;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.service.TransactionRiskService;

@RestController
@RequestMapping("/api/scans/transaction")
public class TransactionController {

    private final TransactionRiskService transactionRiskService;

    public TransactionController(TransactionRiskService transactionRiskService) {
        this.transactionRiskService = transactionRiskService;
    }

    @PostMapping
    public ResponseEntity<TransactionRiskResponse> analyzeTransaction(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody TransactionRiskRequest request
    ) {
        return ResponseEntity.ok(transactionRiskService.analyze(user, request));
    }
}
