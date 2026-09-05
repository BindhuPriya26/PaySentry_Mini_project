package com.paysentry.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.paysentry.backend.entity.UnifiedRiskLevel;
import com.paysentry.backend.repository.FraudAlertRepository;
import com.paysentry.backend.repository.FraudEventRepository;
import com.paysentry.backend.repository.LienChangeRepository;
import com.paysentry.backend.repository.MonitoredTransactionRepository;
import com.paysentry.backend.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/monitoring")
public class MonitoringAdminController {

    private final UserRepository userRepository;
    private final MonitoredTransactionRepository monitoredTransactionRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudEventRepository fraudEventRepository;
    private final LienChangeRepository lienChangeRepository;

    public MonitoringAdminController(
        UserRepository userRepository,
        MonitoredTransactionRepository monitoredTransactionRepository,
        FraudAlertRepository fraudAlertRepository,
        FraudEventRepository fraudEventRepository,
        LienChangeRepository lienChangeRepository
    ) {
        this.userRepository = userRepository;
        this.monitoredTransactionRepository = monitoredTransactionRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudEventRepository = fraudEventRepository;
        this.lienChangeRepository = lienChangeRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Long>> getSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("totalUsers", userRepository.count());
        summary.put("totalTransactions", monitoredTransactionRepository.count());
        summary.put("totalFraudAlerts", fraudAlertRepository.count());
        summary.put("highRiskEvents", fraudEventRepository.countByRiskLevelIn(
            java.util.Arrays.asList(UnifiedRiskLevel.HIGH, UnifiedRiskLevel.CRITICAL)
        ));
        summary.put("lienAlerts", lienChangeRepository.count());
        summary.put("recentSuspiciousActivity", fraudEventRepository.countByRiskLevelIn(
            java.util.Arrays.asList(UnifiedRiskLevel.MEDIUM, UnifiedRiskLevel.HIGH, UnifiedRiskLevel.CRITICAL)
        ));
        return ResponseEntity.ok(summary);
    }
}
