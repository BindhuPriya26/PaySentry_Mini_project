package com.paysentry.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.dto.scan.UrlScanRequest;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.UrlScan;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.repository.UrlScanRepository;

@Service
public class UrlScanService {

    private final UrlScanRepository urlScanRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;

    public UrlScanService(
        UrlScanRepository urlScanRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService
    ) {
        this.urlScanRepository = urlScanRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
    }

    @Transactional
    public FraudScanResponse scan(User user, UrlScanRequest request) {
        RiskAssessment assessment = fraudScoringService.assessUrl(request.getUrl());

        UrlScan scan = new UrlScan();
        scan.setUser(user);
        scan.setUrl(request.getUrl().trim());
        scan.setFindings(String.join(" | ", assessment.getFindings()));
        scan.setRiskLevel(assessment.getRiskLevel());
        scan.setRiskScore(assessment.getRiskScore());
        scan.setExplanation(assessment.getExplanation());

        UrlScan savedScan = urlScanRepository.save(scan);
        reportService.createReport(user, ScanType.URL, savedScan.getId(), "Phishing URL Scan", assessment);

        FraudScanResponse response = new FraudScanResponse();
        response.setScanId(savedScan.getId());
        response.setChannel("URL");
        response.setRiskLevel(savedScan.getRiskLevel().name());
        response.setRiskScore(savedScan.getRiskScore());
        response.setExplanation(savedScan.getExplanation());
        response.setFindings(assessment.getFindings());
        response.setRecommendations(assessment.getRecommendations());
        response.setExtractedUrl(assessment.getExtractedUrl());
        response.setCreatedAt(savedScan.getCreatedAt());
        return response;
    }
}
