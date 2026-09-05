package com.paysentry.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.monitoring.MobileAlertResponse;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.monitoring.AlertService;

@RestController
@RequestMapping("/api/mobile/alerts")
public class MobileAlertController {

    private final AlertService alertService;

    public MobileAlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<MobileAlertResponse>> listAlerts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(alertService.listForUser(user));
    }

    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<MobileAlertResponse> acknowledge(
        @AuthenticationPrincipal User user,
        @PathVariable Long alertId
    ) {
        return ResponseEntity.ok(alertService.acknowledge(user, alertId));
    }
}
