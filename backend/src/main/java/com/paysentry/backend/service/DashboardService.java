package com.paysentry.backend.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.paysentry.backend.dto.dashboard.DashboardActivityResponse;
import com.paysentry.backend.dto.dashboard.DashboardResponse;
import com.paysentry.backend.entity.Report;
import com.paysentry.backend.entity.RiskLevel;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.QrScanRepository;
import com.paysentry.backend.repository.ReportRepository;
import com.paysentry.backend.repository.SmsScanRepository;
import com.paysentry.backend.repository.TransactionRecordRepository;
import com.paysentry.backend.repository.UrlScanRepository;

@Service
public class DashboardService {

    private final SmsScanRepository smsScanRepository;
    private final QrScanRepository qrScanRepository;
    private final UrlScanRepository urlScanRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ReportRepository reportRepository;

    public DashboardService(
        SmsScanRepository smsScanRepository,
        QrScanRepository qrScanRepository,
        UrlScanRepository urlScanRepository,
        TransactionRecordRepository transactionRecordRepository,
        ReportRepository reportRepository
    ) {
        this.smsScanRepository = smsScanRepository;
        this.qrScanRepository = qrScanRepository;
        this.urlScanRepository = urlScanRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.reportRepository = reportRepository;
    }

    public DashboardResponse getDashboard(User user) {
        long smsCount = smsScanRepository.countByUser(user);
        long qrCount = qrScanRepository.countByUser(user);
        long urlCount = urlScanRepository.countByUser(user);
        long transactionCount = transactionRecordRepository.countByUser(user);
        long totalScans = smsCount + qrCount + urlCount + transactionCount;

        List<Report> reports = reportRepository.findByUserOrderByCreatedAtDesc(user);
        long fraudAlerts = reports.stream()
            .filter(report -> report.getRiskLevel() != RiskLevel.SAFE)
            .count();

        double averageRiskScore = reports.stream()
            .mapToInt(Report::getRiskScore)
            .average()
            .orElse(0.0);

        Map<String, Long> scanBreakdown = new LinkedHashMap<>();
        scanBreakdown.put("SMS", smsCount);
        scanBreakdown.put("QR", qrCount);
        scanBreakdown.put("URL", urlCount);
        scanBreakdown.put("TRANSACTION", transactionCount);

        Map<String, Long> riskBreakdown = new LinkedHashMap<>();
        riskBreakdown.put(RiskLevel.SAFE.name(), 0L);
        riskBreakdown.put(RiskLevel.MEDIUM_RISK.name(), 0L);
        riskBreakdown.put(RiskLevel.HIGH_RISK.name(), 0L);
        for (Report report : reports) {
            riskBreakdown.put(report.getRiskLevel().name(), riskBreakdown.get(report.getRiskLevel().name()) + 1);
        }

        List<DashboardActivityResponse> recentActivity = reportRepository.findTop10ByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(this::toActivityResponse)
            .collect(Collectors.toList());

        DashboardResponse response = new DashboardResponse();
        response.setTotalFraudScans(totalScans);
        response.setFraudAlerts(fraudAlerts);
        response.setAverageRiskScore(Math.round(averageRiskScore * 10.0) / 10.0);
        response.setScanBreakdown(scanBreakdown);
        response.setRiskBreakdown(riskBreakdown);
        response.setRecentActivity(recentActivity);
        return response;
    }

    private DashboardActivityResponse toActivityResponse(Report report) {
        DashboardActivityResponse response = new DashboardActivityResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setScanType(report.getScanType().name());
        response.setRiskLevel(report.getRiskLevel().name());
        response.setRiskScore(report.getRiskScore());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }
}
