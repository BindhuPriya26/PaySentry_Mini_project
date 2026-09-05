package com.paysentry.backend.monitoring;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.detection.ContentDetectionResponse;
import com.paysentry.backend.entity.BankAccount;
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.service.FraudScoringService;
import com.paysentry.backend.service.RiskAssessment;

@Service
public class MobileDetectionService {

    private final FraudScoringService fraudScoringService;
    private final BankAccountService bankAccountService;
    private final RiskEngineService riskEngineService;

    public MobileDetectionService(
        FraudScoringService fraudScoringService,
        BankAccountService bankAccountService,
        RiskEngineService riskEngineService
    ) {
        this.fraudScoringService = fraudScoringService;
        this.bankAccountService = bankAccountService;
        this.riskEngineService = riskEngineService;
    }

    @Transactional
    public ContentDetectionResponse analyze(User user, BankingEventType eventType, String content) {
        RiskAssessment assessment = assess(eventType, content);
        BankAccount account = bankAccountService.getForUser(user);
        LocalDateTime analyzedAt = LocalDateTime.now();
        RiskOutcome outcome = riskEngineService.recordSignal(
            user,
            account,
            eventType,
            eventType.name().toLowerCase() + "-" + UUID.randomUUID(),
            assessment.getRiskScore(),
            assessment.getFindings(),
            "content=" + content.trim(),
            analyzedAt
        );

        ContentDetectionResponse response = new ContentDetectionResponse();
        response.setChannel(eventType.name());
        response.setRiskScore(outcome.getScore());
        response.setRiskLevel(outcome.getRiskLevel().name());
        response.setReasons(outcome.getReasons());
        response.setExplanation(assessment.getExplanation());
        response.setRecommendedAction(outcome.getRecommendedAction());
        response.setExtractedContent(eventType == BankingEventType.QR ? content.trim() : assessment.getExtractedUrl());
        response.setAnalyzedAt(analyzedAt);
        return response;
    }

    private RiskAssessment assess(BankingEventType eventType, String content) {
        if (eventType == BankingEventType.SMS) {
            return fraudScoringService.assessSms(content);
        }
        if (eventType == BankingEventType.URL) {
            return fraudScoringService.assessUrl(content);
        }
        return fraudScoringService.assessQrContent(content);
    }
}
