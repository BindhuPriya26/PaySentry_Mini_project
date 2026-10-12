package com.paysentry.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.dto.scan.UrlScanRequest;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.UrlScan;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.monitoring.RiskEngineService;
import com.paysentry.backend.monitoring.RiskOutcome;
import com.paysentry.backend.repository.UrlScanRepository;

@Service
public class UrlScanService {

    private final UrlScanRepository urlScanRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;
    private final BankAccountService bankAccountService;
    private final RiskEngineService riskEngineService;
    private final SensitiveContentService sensitiveContentService;

    public UrlScanService(
        UrlScanRepository urlScanRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService,
        BankAccountService bankAccountService,
        RiskEngineService riskEngineService,
        SensitiveContentService sensitiveContentService
    ) {
        this.urlScanRepository = urlScanRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
        this.bankAccountService = bankAccountService;
        this.riskEngineService = riskEngineService;
        this.sensitiveContentService = sensitiveContentService;
    }

    @Transactional
    public FraudScanResponse scan(User user, UrlScanRequest request) {
        RiskAssessment assessment = fraudScoringService.assessUrl(request.getUrl());

        UrlScan scan = new UrlScan();
        scan.setUser(user);
        scan.setUrl(sensitiveContentService.safeUrlEvidence(assessment.getExtractedUrl()));
        scan.setFindings(String.join(" | ", assessment.getFindings()));
        scan.setRiskLevel(assessment.getRiskLevel());
        scan.setRiskScore(assessment.getRiskScore());
        scan.setExplanation(assessment.getExplanation());

        UrlScan savedScan = urlScanRepository.save(scan);
        RiskOutcome unifiedRisk = riskEngineService.recordSignal(
            user,
            bankAccountService.getForUser(user),
            BankingEventType.URL,
            "url-shared-" + java.util.UUID.randomUUID(),
            assessment.getRiskScore(),
            assessment.getFindings(),
            sensitiveContentService.safeUrlEvidence(assessment.getExtractedUrl()),
            java.time.LocalDateTime.now()
        );
        reportService.createUnifiedReport(user, ScanType.URL, savedScan.getId(), "Phishing URL Scan", unifiedRisk);

        FraudScanResponse response = new FraudScanResponse();
        response.setScanId(savedScan.getId());
        response.setChannel("URL");
        response.setRiskLevel(unifiedRisk.getRiskLevel().name());
        response.setRiskScore(unifiedRisk.getScore());
        response.setExplanation(savedScan.getExplanation());
        response.setFindings(unifiedRisk.getReasons());
        response.setRecommendations(assessment.getRecommendations());
        response.setExtractedUrl(assessment.getExtractedUrl());
        response.setCreatedAt(savedScan.getCreatedAt());
        return response;
    }
}
