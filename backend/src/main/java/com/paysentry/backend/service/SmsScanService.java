package com.paysentry.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.dto.scan.SmsScanRequest;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.SmsScan;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.monitoring.RiskEngineService;
import com.paysentry.backend.monitoring.RiskOutcome;
import com.paysentry.backend.repository.SmsScanRepository;

@Service
public class SmsScanService {

    private final SmsScanRepository smsScanRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;
    private final BankAccountService bankAccountService;
    private final RiskEngineService riskEngineService;
    private final SensitiveContentService sensitiveContentService;

    public SmsScanService(
        SmsScanRepository smsScanRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService,
        BankAccountService bankAccountService,
        RiskEngineService riskEngineService,
        SensitiveContentService sensitiveContentService
    ) {
        this.smsScanRepository = smsScanRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
        this.bankAccountService = bankAccountService;
        this.riskEngineService = riskEngineService;
        this.sensitiveContentService = sensitiveContentService;
    }

    @Transactional
    public FraudScanResponse scan(User user, SmsScanRequest request) {
        if (!request.isConsentConfirmed()) {
            throw new BadRequestException("Confirm that you want to send this SMS to PaySentry for analysis.");
        }
        RiskAssessment assessment = fraudScoringService.assessSms(request.getMessageContent());

        SmsScan scan = new SmsScan();
        scan.setUser(user);
        scan.setMessageContent(sensitiveContentService.redactedSmsEvidence(request.getMessageContent()));
        scan.setSuspiciousKeywords(String.join(" | ", assessment.getFindings()));
        scan.setRiskLevel(assessment.getRiskLevel());
        scan.setRiskScore(assessment.getRiskScore());
        scan.setExplanation(assessment.getExplanation());

        SmsScan savedScan = smsScanRepository.save(scan);
        RiskOutcome unifiedRisk = riskEngineService.recordSignal(
            user,
            bankAccountService.getForUser(user),
            BankingEventType.SMS,
            "sms-shared-" + java.util.UUID.randomUUID(),
            assessment.getRiskScore(),
            assessment.getFindings(),
            "contentFingerprint=" + sensitiveContentService.fingerprint(request.getMessageContent()),
            java.time.LocalDateTime.now()
        );
        reportService.createUnifiedReport(user, ScanType.SMS, savedScan.getId(), "SMS Fraud Scan", unifiedRisk);

        FraudScanResponse response = new FraudScanResponse();
        response.setScanId(savedScan.getId());
        response.setChannel("SMS");
        response.setRiskLevel(unifiedRisk.getRiskLevel().name());
        response.setRiskScore(unifiedRisk.getScore());
        response.setExplanation(savedScan.getExplanation());
        response.setFindings(unifiedRisk.getReasons());
        response.setRecommendations(assessment.getRecommendations());
        response.setCreatedAt(savedScan.getCreatedAt());
        return response;
    }
}
