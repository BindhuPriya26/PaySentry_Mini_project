package com.paysentry.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.paysentry.backend.entity.RiskLevel;

class FraudScoringServiceTests {

    private final FraudScoringService fraudScoringService = new FraudScoringService();

    @Test
    void suspiciousHostnameShouldNotBeMarkedSafe() {
        RiskAssessment assessment = fraudScoringService.assessUrl("https://secure-bank-update.example/login");

        assertEquals(RiskLevel.MEDIUM_RISK, assessment.getRiskLevel());
        assertTrue(assessment.getRiskScore() >= 35);
    }

    @Test
    void ipBasedHttpUrlShouldBeMarkedHighRisk() {
        RiskAssessment assessment = fraudScoringService.assessUrl("http://198.51.100.25/verify");

        assertEquals(RiskLevel.HIGH_RISK, assessment.getRiskLevel());
        assertTrue(assessment.getRiskScore() >= 70);
    }

    @Test
    void ordinaryHttpsUrlCanStillBeSafe() {
        RiskAssessment assessment = fraudScoringService.assessUrl("https://www.example.com/help");

        assertEquals(RiskLevel.SAFE, assessment.getRiskLevel());
        assertTrue(assessment.getRiskScore() < 35);
    }
}
