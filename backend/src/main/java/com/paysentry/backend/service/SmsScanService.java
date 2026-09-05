package com.paysentry.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.dto.scan.SmsScanRequest;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.SmsScan;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.SmsScanRepository;

@Service
public class SmsScanService {

    private final SmsScanRepository smsScanRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;

    public SmsScanService(
        SmsScanRepository smsScanRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService
    ) {
        this.smsScanRepository = smsScanRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
    }

    @Transactional
    public FraudScanResponse scan(User user, SmsScanRequest request) {
        RiskAssessment assessment = fraudScoringService.assessSms(request.getMessageContent());

        SmsScan scan = new SmsScan();
        scan.setUser(user);
        scan.setMessageContent(request.getMessageContent().trim());
        scan.setSuspiciousKeywords(String.join(" | ", assessment.getFindings()));
        scan.setRiskLevel(assessment.getRiskLevel());
        scan.setRiskScore(assessment.getRiskScore());
        scan.setExplanation(assessment.getExplanation());

        SmsScan savedScan = smsScanRepository.save(scan);
        reportService.createReport(user, ScanType.SMS, savedScan.getId(), "SMS Fraud Scan", assessment);

        FraudScanResponse response = new FraudScanResponse();
        response.setScanId(savedScan.getId());
        response.setChannel("SMS");
        response.setRiskLevel(savedScan.getRiskLevel().name());
        response.setRiskScore(savedScan.getRiskScore());
        response.setExplanation(savedScan.getExplanation());
        response.setFindings(assessment.getFindings());
        response.setRecommendations(assessment.getRecommendations());
        response.setCreatedAt(savedScan.getCreatedAt());
        return response;
    }
}
