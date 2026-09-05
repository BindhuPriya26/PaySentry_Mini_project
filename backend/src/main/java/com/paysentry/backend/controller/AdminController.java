package com.paysentry.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.dto.admin.SystemStatsResponse;
import com.paysentry.backend.dto.admin.UserSummaryResponse;
import com.paysentry.backend.dto.report.ReportResponse;
import com.paysentry.backend.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserSummaryResponse> updateUserStatus(
        @PathVariable Long userId,
        @RequestParam boolean active
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, active));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ReportResponse>> getReports() {
        return ResponseEntity.ok(adminService.listReports());
    }

    @GetMapping("/stats")
    public ResponseEntity<SystemStatsResponse> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }
}
