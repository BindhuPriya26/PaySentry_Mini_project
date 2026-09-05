package com.paysentry.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.monitoring.MobileDashboardResponse;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.monitoring.MobileDashboardService;

@RestController
@RequestMapping("/api/mobile/dashboard")
public class MobileDashboardController {

    private final MobileDashboardService mobileDashboardService;

    public MobileDashboardController(MobileDashboardService mobileDashboardService) {
        this.mobileDashboardService = mobileDashboardService;
    }

    @GetMapping
    public ResponseEntity<MobileDashboardResponse> getDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mobileDashboardService.getDashboard(user));
    }
}
